package com.eazicut.api.cart.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eazicut.api.cart.dto.AddCartItemRequest;
import com.eazicut.api.cart.dto.CartItemResponse;
import com.eazicut.api.cart.dto.CartResponse;
import com.eazicut.api.cart.dto.UpdateCartItemRequest;
import com.eazicut.api.cart.entity.Cart;
import com.eazicut.api.cart.entity.CartItem;
import com.eazicut.api.cart.exception.CartLineNotFoundException;
import com.eazicut.api.cart.exception.CartTooLargeException;
import com.eazicut.api.cart.exception.InsufficientStockException;
import com.eazicut.api.cart.exception.ProductUnavailableException;
import com.eazicut.api.cart.exception.SizeUnavailableException;
import com.eazicut.api.cart.mapper.CartMapper;
import com.eazicut.api.cart.repository.CartItemRepository;
import com.eazicut.api.cart.repository.CartRepository;
import com.eazicut.api.common.exception.ResourceNotFoundException;
import com.eazicut.api.products.entity.Product;
import com.eazicut.api.products.entity.ProductStatus;
import com.eazicut.api.products.repository.ProductRepository;
import com.eazicut.api.users.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * Cart application service.
 *
 * <p>Read surface (Stage 1): {@link #getOrCreate}, {@link #toResponse}.
 * Mutating surface (Stage 2): {@link #add}, {@link #setQuantity},
 * {@link #remove}, {@link #clear}. Every mutating method returns the
 * updated cart via {@link #toResponse} so a controller round-trip
 * yields exactly what {@code GET /cart} would return.
 *
 * <p><strong>Ownership.</strong> The service NEVER accepts a cart id
 * from a caller. Every entry point takes a {@link User} — resolved
 * from the authenticated principal in the controller — and derives
 * the cart via {@link CartRepository#findByUserId}. Item-id-based
 * operations scope the lookup with
 * {@code CartItemRepository.findByIdAndCartId} so a caller can never
 * touch a line that isn't in their own cart (a fabricated or
 * enumerated id resolves to a 404, not a 401 or 403 — this closes the
 * enumeration side channel).
 *
 * <p><strong>Add-or-increment.</strong> A POST for a product+size the
 * caller already has bumps the existing line's quantity (capped at
 * {@link #PER_LINE_QTY_CAP}). Ideal for the frontend's "Add to Bag"
 * button firing multiple times.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CartService {

    /**
     * Cart currency at launch. Multi-currency support means moving
     * this off the cart and onto the line — deferred until we actually
     * quote more than NGN.
     */
    private static final String DEFAULT_CURRENCY = "NGN";

    /** Per-line quantity cap. Mirrors the DB CHECK in V6. */
    static final int PER_LINE_QTY_CAP = 20;

    /** Per-cart distinct-line cap. Enforced in service only. */
    static final int PER_CART_LINE_CAP = 50;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final ProductRepository productRepository;

    // ---------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------

    /**
     * Return the caller's cart, creating an empty one on first access.
     *
     * <p>Concurrent-first-access race: two requests from the same
     * fresh account can both miss the SELECT and race the INSERT. The
     * {@code ux_cart_user} unique index is the backstop — the loser's
     * INSERT throws {@code DataIntegrityViolationException} which the
     * GlobalExceptionHandler surfaces as 409. Caller can retry;
     * subsequent calls hit the row that survived.
     */
    public Cart getOrCreate(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> createEmpty(user));
    }

    /**
     * Single read-path entry — resolves the cart (creating if needed)
     * and produces the wire {@link CartResponse} inside one
     * transaction. Callers should prefer this over calling
     * {@link #getOrCreate} + {@link #toResponse} separately, because
     * the mapper touches lazy fields on {@link com.eazicut.api.products.entity.Product}
     * (notably {@code availableSizes}) that need an active session.
     */
    @Transactional(readOnly = true)
    public CartResponse readForUser(User user) {
        return toResponse(getOrCreate(user));
    }

    /**
     * Read-only projection into the wire shape.
     *
     * <p>The subtotal is the sum of {@code snapshotPrice × quantity}
     * across every line — historical value. The charged amount is
     * decided at Order time (see {@code CartItem} Javadoc).
     *
     * <p>Must be called within an active JPA session — see
     * {@link #readForUser} for the correct entry-point pattern.
     */
    @Transactional(readOnly = true)
    public CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(cartMapper::toItemResponse)
                .toList();

        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getSnapshotPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int itemCount = cart.getItems().stream().mapToInt(item -> item.getQuantity()).sum();

        return new CartResponse(
                cart.getId(),
                cart.getCurrency(),
                items,
                subtotal,
                itemCount,
                Collections.emptyList(),   // populated in Stage 3
                cart.getUpdatedAt()
        );
    }

    // ---------------------------------------------------------------------
    // Mutations
    // ---------------------------------------------------------------------

    /**
     * Add a line, or bump the quantity of an existing matching one.
     *
     * <p>Validation order (all failures throw before any mutation):
     * <ol>
     *   <li>Product exists and is not soft-deleted (404).</li>
     *   <li>Product status is {@code ACTIVE} (409).</li>
     *   <li>Requested size is in {@code product.availableSizes} (409).</li>
     *   <li>Cart line cap not exceeded (413).</li>
     *   <li>Requested new total quantity does not exceed stock (409).</li>
     * </ol>
     */
    public CartResponse add(User user, AddCartItemRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId()));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ProductUnavailableException(product.getSlug());
        }
        if (product.getAvailableSizes() == null || !product.getAvailableSizes().contains(request.size())) {
            throw new SizeUnavailableException(product.getSlug(), request.size());
        }

        Cart cart = getOrCreate(user);
        CartItem existing = cartItemRepository
                .findByCartIdAndProductIdAndSize(cart.getId(), product.getId(), request.size())
                .orElse(null);

        int nextQty;
        if (existing != null) {
            nextQty = clampedTotal(existing.getQuantity(), request.quantity());
            assertStock(product, request.size(), nextQty);
            existing.setQuantity(nextQty);
        } else {
            assertCartCapacity(cart);
            nextQty = Math.min(request.quantity(), PER_LINE_QTY_CAP);
            assertStock(product, request.size(), nextQty);
            CartItem line = newLine(cart, product, request.size(), nextQty);
            cart.getItems().add(line);
        }

        cartRepository.saveAndFlush(cart);
        return toResponse(cart);
    }

    /**
     * Set a line's absolute quantity (not a delta). Zero is not
     * accepted — use {@link #remove} for that (the DTO enforces min=1).
     */
    public CartResponse setQuantity(User user, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreate(user);
        CartItem line = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new CartLineNotFoundException(itemId));

        int requested = Math.min(request.quantity(), PER_LINE_QTY_CAP);
        assertStock(line.getProduct(), line.getSize(), requested);

        line.setQuantity(requested);
        cartRepository.saveAndFlush(cart);
        return toResponse(cart);
    }

    /**
     * Remove a single line. Silent no-op on a missing id would leak the
     * "does this id exist somewhere else" bit — we return 404 instead,
     * so an enumeration attempt looks identical to a legitimate typo.
     */
    public CartResponse remove(User user, UUID itemId) {
        Cart cart = getOrCreate(user);
        CartItem line = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new CartLineNotFoundException(itemId));

        cart.getItems().remove(line);
        cartRepository.saveAndFlush(cart);
        return toResponse(cart);
    }

    /**
     * Empty every line. Returns the empty cart shape (id preserved).
     */
    public CartResponse clear(User user) {
        Cart cart = getOrCreate(user);
        cart.getItems().clear();
        cartRepository.saveAndFlush(cart);
        return toResponse(cart);
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private Cart createEmpty(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setCurrency(DEFAULT_CURRENCY);
        return cartRepository.save(cart);
    }

    private CartItem newLine(Cart cart, Product product, String size, int quantity) {
        CartItem line = new CartItem();
        line.setCart(cart);
        line.setProduct(product);
        line.setSize(size);
        line.setQuantity(quantity);
        line.setSnapshotName(product.getName());
        line.setSnapshotSlug(product.getSlug());
        line.setSnapshotPrice(product.getPrice());
        line.setSnapshotCurrency(product.getCurrency());
        line.setSnapshotImageUrl(primaryImageUrl(product));
        Instant now = Instant.now();
        line.setAddedAt(now);
        line.setUpdatedAt(now);
        return line;
    }

    private static String primaryImageUrl(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) return null;
        return product.getImages().stream()
                .filter(img -> img.isPrimary())
                .findFirst()
                .orElse(product.getImages().get(0))
                .getUrl();
    }

    private static int clampedTotal(int existing, int adding) {
        return Math.min(existing + adding, PER_LINE_QTY_CAP);
    }

    private void assertCartCapacity(Cart cart) {
        if (cart.getItems().size() >= PER_CART_LINE_CAP) {
            throw new CartTooLargeException(cart.getItems().size(), PER_CART_LINE_CAP);
        }
    }

    private void assertStock(Product product, String size, int requested) {
        if (product.getStockQuantity() < requested) {
            throw new InsufficientStockException(product.getSlug(), requested, product.getStockQuantity());
        }
        // Size availability is re-checked here for setQuantity — the merchant
        // may have retired a size while the line sat in the cart.
        if (product.getAvailableSizes() == null || !product.getAvailableSizes().contains(size)) {
            throw new SizeUnavailableException(product.getSlug(), size);
        }
    }
}
