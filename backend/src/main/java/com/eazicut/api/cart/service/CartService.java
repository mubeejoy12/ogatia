package com.eazicut.api.cart.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eazicut.api.cart.dto.AddCartItemRequest;
import com.eazicut.api.cart.dto.CartIssueResponse;
import com.eazicut.api.cart.dto.CartItemResponse;
import com.eazicut.api.cart.dto.CartResponse;
import com.eazicut.api.cart.dto.MergeCartRequest;
import com.eazicut.api.cart.dto.MergeCartRequest.GuestLine;
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
    private final CartIssueDetector issueDetector;

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

        List<CartIssueResponse> issues = issueDetector.detect(cart);

        return new CartResponse(
                cart.getId(),
                cart.getCurrency(),
                items,
                subtotal,
                itemCount,
                issues,
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

    /**
     * Merge a guest (localStorage) cart into the authenticated user's
     * server-side cart. Called by the frontend immediately after login.
     *
     * <p><strong>Merge rules per line:</strong>
     * <ol>
     *   <li>Resolve {@code productSlug} to a Product — skip if
     *       unknown, soft-deleted, or {@code status != ACTIVE}. Record
     *       a {@code product_removed} issue.</li>
     *   <li>Verify the size is in {@code product.availableSizes} —
     *       skip and record {@code size_unavailable} otherwise.</li>
     *   <li>Upsert on {@code (cart, product, size)}:
     *     <ul>
     *       <li>If existing: {@code new_qty = min(existing + incoming, 20)}.
     *           Record {@code quantity_capped} if the sum was trimmed.</li>
     *       <li>If not: enforce the per-cart line cap (50). If full,
     *           skip this line and record a single
     *           {@code cart_too_large} issue for the whole overflow.
     *           Otherwise insert with the current product's snapshot
     *           and {@code new_qty = min(incoming, 20)}.</li>
     *     </ul>
     *   </li>
     *   <li>Stock is intentionally NOT enforced here — a guest may
     *       have added an item when stock was 20 and we're merging
     *       after stock dropped to 2. Better to accept the line and
     *       let {@code CartIssueDetector} flag it with
     *       {@code out_of_stock} on the response, so the customer
     *       sees "you have 5 in cart but only 2 available" instead
     *       of "your item was silently dropped".</li>
     * </ol>
     *
     * <p><strong>Never fails hard on a single guest line.</strong> A
     * malformed guest cart never blocks the merge — every skip goes to
     * {@code issues[]}. The customer always ends up with a usable
     * server-side cart.
     */
    public CartResponse merge(User user, MergeCartRequest request) {
        Cart cart = getOrCreate(user);
        List<CartIssueResponse> mergeIssues = new ArrayList<>();

        List<GuestLine> lines = request.lines() == null ? List.of() : request.lines();
        int cartTooLargeOverflow = 0;

        for (GuestLine guest : lines) {
            Product product = productRepository.findBySlug(guest.productSlug()).orElse(null);
            if (product == null || product.getStatus() != ProductStatus.ACTIVE) {
                mergeIssues.add(new CartIssueResponse(null, "product_removed",
                        "'%s' is no longer available in the atelier's catalogue."
                                .formatted(guest.productSlug())));
                continue;
            }
            if (product.getAvailableSizes() == null
                    || !product.getAvailableSizes().contains(guest.size())) {
                mergeIssues.add(new CartIssueResponse(null, "size_unavailable",
                        "Size '%s' is no longer offered for '%s'."
                                .formatted(guest.size(), product.getName())));
                continue;
            }

            CartItem existing = cartItemRepository
                    .findByCartIdAndProductIdAndSize(cart.getId(), product.getId(), guest.size())
                    .orElse(null);

            int desired = guest.quantity();
            if (existing != null) {
                int summed = existing.getQuantity() + desired;
                int clamped = Math.min(summed, PER_LINE_QTY_CAP);
                if (clamped < summed) {
                    mergeIssues.add(new CartIssueResponse(existing.getId(), "quantity_capped",
                            "'%s' quantity capped at %d.".formatted(product.getName(), PER_LINE_QTY_CAP)));
                }
                existing.setQuantity(clamped);
            } else {
                if (cart.getItems().size() >= PER_CART_LINE_CAP) {
                    cartTooLargeOverflow++;
                    continue;
                }
                int clamped = Math.min(desired, PER_LINE_QTY_CAP);
                if (clamped < desired) {
                    // No item id yet — the line is being created now.
                    mergeIssues.add(new CartIssueResponse(null, "quantity_capped",
                            "'%s' quantity capped at %d.".formatted(product.getName(), PER_LINE_QTY_CAP)));
                }
                CartItem line = newLine(cart, product, guest.size(), clamped);
                cart.getItems().add(line);
            }
        }

        if (cartTooLargeOverflow > 0) {
            mergeIssues.add(new CartIssueResponse(null, "cart_too_large",
                    "%d line%s dropped — the cart line cap is %d."
                            .formatted(cartTooLargeOverflow,
                                    cartTooLargeOverflow == 1 ? "" : "s",
                                    PER_CART_LINE_CAP)));
        }

        cartRepository.saveAndFlush(cart);

        // Merge the read-time issues from the detector with the merge-time
        // issues we accumulated above. Merge issues carry itemId=null when
        // the line was skipped or newly created — the frontend renders them
        // as cart-level toasts rather than per-line badges.
        CartResponse base = toResponse(cart);
        List<CartIssueResponse> combined = new ArrayList<>(base.issues().size() + mergeIssues.size());
        combined.addAll(mergeIssues);
        combined.addAll(base.issues());
        return new CartResponse(
                base.id(),
                base.currency(),
                base.items(),
                base.subtotal(),
                base.itemCount(),
                combined,
                base.updatedAt()
        );
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
