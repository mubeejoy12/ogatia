package com.eazicut.api.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eazicut.api.cart.dto.AddCartItemRequest;
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
import com.eazicut.api.cart.mapper.CartMapperImpl;
import com.eazicut.api.cart.repository.CartItemRepository;
import com.eazicut.api.cart.repository.CartRepository;
import com.eazicut.api.common.exception.ResourceNotFoundException;
import com.eazicut.api.products.entity.Product;
import com.eazicut.api.products.entity.ProductStatus;
import com.eazicut.api.products.repository.ProductRepository;
import com.eazicut.api.users.entity.Role;
import com.eazicut.api.users.entity.User;

/**
 * Unit tests for {@link CartService} — Stage 1 read-only surface.
 *
 * <p>Uses the generated {@link CartMapperImpl} directly (MapStruct
 * emits a plain class with no Spring dependencies) so the mapping
 * behaviour is exercised alongside the service logic.
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;

    private final CartMapper mapper = new CartMapperImpl();
    private final CartIssueDetector issueDetector = new CartIssueDetector();

    private CartService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new CartService(cartRepository, cartItemRepository, mapper, productRepository, issueDetector);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("customer@example.com");
        user.setEmailLower("customer@example.com");
        user.setPasswordHash("$2a$10$fake");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
    }

    @Test
    @DisplayName("getOrCreate — returns the existing cart when the user already has one")
    void getOrCreateReturnsExisting() {
        Cart existing = new Cart();
        existing.setId(UUID.randomUUID());
        existing.setUser(user);
        existing.setCurrency("NGN");
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(existing));

        Cart result = service.getOrCreate(user);

        assertThat(result).isSameAs(existing);
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreate — persists a new empty NGN cart when the user has none")
    void getOrCreateCreatesWhenMissing() {
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.empty());
        given(cartRepository.save(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        Cart created = service.getOrCreate(user);

        assertThat(created.getUser()).isSameAs(user);
        assertThat(created.getCurrency()).isEqualTo("NGN");
        assertThat(created.getItems()).isEmpty();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("toResponse — empty cart yields itemCount 0, subtotal 0, empty issues")
    void toResponseEmpty() {
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUser(user);
        cart.setCurrency("NGN");
        cart.setUpdatedAt(Instant.now());

        CartResponse resp = service.toResponse(cart);

        assertThat(resp.id()).isEqualTo(cart.getId());
        assertThat(resp.currency()).isEqualTo("NGN");
        assertThat(resp.items()).isEmpty();
        assertThat(resp.itemCount()).isZero();
        assertThat(resp.subtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.issues()).isEmpty();
    }

    @Test
    @DisplayName("toResponse — subtotal is snapshot_price × quantity summed across lines (historical value)")
    void toResponseSubtotal() {
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUser(user);
        cart.setCurrency("NGN");
        cart.setUpdatedAt(Instant.now());

        Product product = product("test-piece", "Test Piece", new BigDecimal("100000"), ProductStatus.ACTIVE, 10);
        cart.setItems(List.of(
                item(cart, product, "L", 2, new BigDecimal("100000")),
                item(cart, product, "XL", 1, new BigDecimal("150000"))
        ));

        CartResponse resp = service.toResponse(cart);

        // 100000 × 2 + 150000 × 1 = 350000
        assertThat(resp.subtotal()).isEqualByComparingTo(new BigDecimal("350000"));
        assertThat(resp.itemCount()).isEqualTo(3);
        assertThat(resp.items()).hasSize(2);
    }

    @Test
    @DisplayName("toResponse — subtotal uses SNAPSHOT price, not current product price (historical invariant)")
    void toResponseUsesSnapshotNotCurrent() {
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUser(user);
        cart.setCurrency("NGN");
        cart.setUpdatedAt(Instant.now());

        // Product's current price is 200000; snapshot captured at 100000
        Product product = product("test-piece", "Test Piece", new BigDecimal("200000"), ProductStatus.ACTIVE, 10);
        cart.setItems(List.of(
                item(cart, product, "L", 3, new BigDecimal("100000"))
        ));

        CartResponse resp = service.toResponse(cart);

        // Subtotal must use the snapshot (100000), not the current 200000.
        // Stage 3 will surface the delta as a price_changed issue; the
        // charged price is only decided at Order time.
        assertThat(resp.subtotal()).isEqualByComparingTo(new BigDecimal("300000"));

        // The line's currentPrice field still reflects the live product price.
        assertThat(resp.items().get(0).currentPrice()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(resp.items().get(0).snapshot().price()).isEqualByComparingTo(new BigDecimal("100000"));
    }

    @Test
    @DisplayName("toResponse — 'available' is true only when ACTIVE + enough stock + size on offer")
    void toResponseAvailability() {
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUser(user);
        cart.setCurrency("NGN");
        cart.setUpdatedAt(Instant.now());

        Product active = product("a", "A", new BigDecimal("100"), ProductStatus.ACTIVE, 5);
        active.setAvailableSizes(Set.of("L", "XL"));

        Product oos = product("b", "B", new BigDecimal("100"), ProductStatus.OUT_OF_STOCK, 0);
        oos.setAvailableSizes(Set.of("L"));

        Product understocked = product("c", "C", new BigDecimal("100"), ProductStatus.ACTIVE, 1);
        understocked.setAvailableSizes(Set.of("L"));

        cart.setItems(List.of(
                item(cart, active, "L", 2, new BigDecimal("100")),   // available
                item(cart, oos, "L", 1, new BigDecimal("100")),      // NOT available (status)
                item(cart, understocked, "L", 2, new BigDecimal("100")), // NOT available (stock<qty)
                item(cart, active, "XXL", 1, new BigDecimal("100"))  // NOT available (size gone)
        ));

        CartResponse resp = service.toResponse(cart);
        assertThat(resp.items().get(0).available()).isTrue();
        assertThat(resp.items().get(1).available()).isFalse();
        assertThat(resp.items().get(2).available()).isFalse();
        assertThat(resp.items().get(3).available()).isFalse();
    }

    // ---------------------------------------------------------------------
    // Mutations — add / setQuantity / remove / clear
    // ---------------------------------------------------------------------

    private Cart emptyCart() {
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUser(user);
        cart.setCurrency("NGN");
        cart.setUpdatedAt(Instant.now());
        cart.setItems(new ArrayList<>());
        return cart;
    }

    @Test
    @DisplayName("add — first line inserts with snapshot; subtotal reflects it")
    void addFirstLine() {
        Cart cart = emptyCart();
        Product product = product("test-piece", "Test Piece", new BigDecimal("100000"), ProductStatus.ACTIVE, 10);
        AddCartItemRequest req = new AddCartItemRequest(product.getId(), "L", 2);

        given(productRepository.findById(product.getId())).willReturn(Optional.of(product));
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartIdAndProductIdAndSize(cart.getId(), product.getId(), "L"))
                .willReturn(Optional.empty());
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        CartResponse resp = service.add(user, req);

        assertThat(resp.items()).hasSize(1);
        assertThat(resp.items().get(0).quantity()).isEqualTo(2);
        assertThat(resp.items().get(0).snapshot().name()).isEqualTo("Test Piece");
        assertThat(resp.subtotal()).isEqualByComparingTo(new BigDecimal("200000"));
    }

    @Test
    @DisplayName("add — second call on same (product, size) increments the existing line quantity")
    void addBumpsExisting() {
        Cart cart = emptyCart();
        Product product = product("test-piece", "Test Piece", new BigDecimal("100000"), ProductStatus.ACTIVE, 10);
        CartItem existing = item(cart, product, "L", 1, product.getPrice());
        cart.getItems().add(existing);

        AddCartItemRequest req = new AddCartItemRequest(product.getId(), "L", 2);

        given(productRepository.findById(product.getId())).willReturn(Optional.of(product));
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartIdAndProductIdAndSize(cart.getId(), product.getId(), "L"))
                .willReturn(Optional.of(existing));
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        CartResponse resp = service.add(user, req);

        assertThat(resp.items()).hasSize(1);
        assertThat(resp.items().get(0).quantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("add — increment is capped at PER_LINE_QTY_CAP (20)")
    void addCapsAtLineLimit() {
        Cart cart = emptyCart();
        Product product = product("test-piece", "Test Piece", new BigDecimal("100000"), ProductStatus.ACTIVE, 100);
        CartItem existing = item(cart, product, "L", 18, product.getPrice());
        cart.getItems().add(existing);

        AddCartItemRequest req = new AddCartItemRequest(product.getId(), "L", 10);

        given(productRepository.findById(product.getId())).willReturn(Optional.of(product));
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartIdAndProductIdAndSize(cart.getId(), product.getId(), "L"))
                .willReturn(Optional.of(existing));
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        CartResponse resp = service.add(user, req);

        assertThat(resp.items().get(0).quantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("add — unknown productId → ResourceNotFoundException (404)")
    void addUnknownProduct() {
        UUID id = UUID.randomUUID();
        given(productRepository.findById(id)).willReturn(Optional.empty());
        AddCartItemRequest req = new AddCartItemRequest(id, "L", 1);

        assertThatThrownBy(() -> service.add(user, req))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(cartRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("add — product not ACTIVE → ProductUnavailableException (409)")
    void addProductNotActive() {
        Product product = product("test-piece", "Test Piece", new BigDecimal("100000"), ProductStatus.OUT_OF_STOCK, 5);
        given(productRepository.findById(product.getId())).willReturn(Optional.of(product));
        AddCartItemRequest req = new AddCartItemRequest(product.getId(), "L", 1);

        assertThatThrownBy(() -> service.add(user, req))
                .isInstanceOf(ProductUnavailableException.class);
    }

    @Test
    @DisplayName("add — size not offered → SizeUnavailableException (409)")
    void addSizeNotOffered() {
        Product product = product("test-piece", "Test Piece", new BigDecimal("100000"), ProductStatus.ACTIVE, 5);
        given(productRepository.findById(product.getId())).willReturn(Optional.of(product));
        AddCartItemRequest req = new AddCartItemRequest(product.getId(), "XXL", 1);

        assertThatThrownBy(() -> service.add(user, req))
                .isInstanceOf(SizeUnavailableException.class);
    }

    @Test
    @DisplayName("add — requested qty exceeds stock → InsufficientStockException (409)")
    void addExceedsStock() {
        Product product = product("test-piece", "Test Piece", new BigDecimal("100000"), ProductStatus.ACTIVE, 2);
        AddCartItemRequest req = new AddCartItemRequest(product.getId(), "L", 5);

        given(productRepository.findById(product.getId())).willReturn(Optional.of(product));
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(emptyCart()));
        given(cartItemRepository.findByCartIdAndProductIdAndSize(any(), any(), any()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(user, req))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Only 2");
    }

    @Test
    @DisplayName("add — 51st line rejected with CartTooLargeException (413)")
    void addCartTooLarge() {
        Cart cart = emptyCart();
        for (int i = 0; i < 50; i++) {
            Product p = product("p-" + i, "P " + i, new BigDecimal("100"), ProductStatus.ACTIVE, 10);
            cart.getItems().add(item(cart, p, "L", 1, p.getPrice()));
        }
        Product product = product("new-piece", "New Piece", new BigDecimal("100"), ProductStatus.ACTIVE, 10);
        AddCartItemRequest req = new AddCartItemRequest(product.getId(), "L", 1);

        given(productRepository.findById(product.getId())).willReturn(Optional.of(product));
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartIdAndProductIdAndSize(any(), any(), any()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(user, req))
                .isInstanceOf(CartTooLargeException.class)
                .hasMessageContaining("50");
    }

    @Test
    @DisplayName("setQuantity — happy path sets the absolute quantity")
    void setQuantityHappy() {
        Cart cart = emptyCart();
        Product product = product("test-piece", "Test Piece", new BigDecimal("100000"), ProductStatus.ACTIVE, 10);
        CartItem existing = item(cart, product, "L", 1, product.getPrice());
        cart.getItems().add(existing);

        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(cartItemRepository.findByIdAndCartId(existing.getId(), cart.getId()))
                .willReturn(Optional.of(existing));
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        CartResponse resp = service.setQuantity(user, existing.getId(), new UpdateCartItemRequest(4));

        assertThat(resp.items().get(0).quantity()).isEqualTo(4);
    }

    @Test
    @DisplayName("setQuantity — unknown itemId → CartLineNotFoundException (404)")
    void setQuantityUnknown() {
        Cart cart = emptyCart();
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(cartItemRepository.findByIdAndCartId(any(), any())).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.setQuantity(user, UUID.randomUUID(), new UpdateCartItemRequest(2)))
                .isInstanceOf(CartLineNotFoundException.class);
    }

    @Test
    @DisplayName("setQuantity — request qty above stock → InsufficientStockException (409)")
    void setQuantityAboveStock() {
        Cart cart = emptyCart();
        Product product = product("test-piece", "Test Piece", new BigDecimal("100000"), ProductStatus.ACTIVE, 2);
        CartItem existing = item(cart, product, "L", 1, product.getPrice());
        cart.getItems().add(existing);

        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(cartItemRepository.findByIdAndCartId(existing.getId(), cart.getId()))
                .willReturn(Optional.of(existing));

        assertThatThrownBy(() ->
                service.setQuantity(user, existing.getId(), new UpdateCartItemRequest(5)))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("remove — happy path pops the line")
    void removeHappy() {
        Cart cart = emptyCart();
        Product product = product("test-piece", "Test Piece", new BigDecimal("100000"), ProductStatus.ACTIVE, 10);
        CartItem existing = item(cart, product, "L", 1, product.getPrice());
        cart.getItems().add(existing);

        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(cartItemRepository.findByIdAndCartId(existing.getId(), cart.getId()))
                .willReturn(Optional.of(existing));
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        CartResponse resp = service.remove(user, existing.getId());

        assertThat(resp.items()).isEmpty();
    }

    @Test
    @DisplayName("remove — unknown itemId → CartLineNotFoundException (404)")
    void removeUnknown() {
        Cart cart = emptyCart();
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(cartItemRepository.findByIdAndCartId(any(), any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(user, UUID.randomUUID()))
                .isInstanceOf(CartLineNotFoundException.class);
    }

    @Test
    @DisplayName("clear — empties every line")
    void clear() {
        Cart cart = emptyCart();
        Product product = product("test-piece", "Test Piece", new BigDecimal("100000"), ProductStatus.ACTIVE, 10);
        cart.getItems().add(item(cart, product, "L", 1, product.getPrice()));
        cart.getItems().add(item(cart, product, "XL", 2, product.getPrice()));

        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        CartResponse resp = service.clear(user);

        assertThat(resp.items()).isEmpty();
        assertThat(resp.subtotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ---------------------------------------------------------------------
    // Merge (Stage 4)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("merge — empty payload is a no-op; existing cart untouched")
    void mergeEmpty() {
        Cart cart = emptyCart();
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        CartResponse resp = service.merge(user, new MergeCartRequest(List.of()));

        assertThat(resp.items()).isEmpty();
        assertThat(resp.issues()).isEmpty();
    }

    @Test
    @DisplayName("merge — happy path inserts a fresh line with snapshot")
    void mergeFreshLine() {
        Cart cart = emptyCart();
        Product product = product("test-piece", "Test Piece", new BigDecimal("100000"), ProductStatus.ACTIVE, 10);
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(productRepository.findBySlug("test-piece")).willReturn(Optional.of(product));
        given(cartItemRepository.findByCartIdAndProductIdAndSize(cart.getId(), product.getId(), "L"))
                .willReturn(Optional.empty());
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        CartResponse resp = service.merge(user, new MergeCartRequest(List.of(
                new GuestLine("test-piece", "L", 2)
        )));

        assertThat(resp.items()).hasSize(1);
        assertThat(resp.items().get(0).quantity()).isEqualTo(2);
        assertThat(resp.issues()).isEmpty();
    }

    @Test
    @DisplayName("merge — existing line is summed with incoming and capped at 20")
    void mergeSumsAndCaps() {
        Cart cart = emptyCart();
        Product product = product("test-piece", "Test Piece", new BigDecimal("100000"), ProductStatus.ACTIVE, 100);
        CartItem existing = item(cart, product, "L", 15, product.getPrice());
        cart.getItems().add(existing);

        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(productRepository.findBySlug("test-piece")).willReturn(Optional.of(product));
        given(cartItemRepository.findByCartIdAndProductIdAndSize(cart.getId(), product.getId(), "L"))
                .willReturn(Optional.of(existing));
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        CartResponse resp = service.merge(user, new MergeCartRequest(List.of(
                new GuestLine("test-piece", "L", 10)
        )));

        assertThat(resp.items().get(0).quantity()).isEqualTo(20);
        assertThat(resp.issues())
                .extracting(i -> i.code())
                .contains("quantity_capped");
    }

    @Test
    @DisplayName("merge — unknown slug is skipped with product_removed issue (payload doesn't fail)")
    void mergeUnknownSlug() {
        Cart cart = emptyCart();
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(productRepository.findBySlug("ghost")).willReturn(Optional.empty());
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        CartResponse resp = service.merge(user, new MergeCartRequest(List.of(
                new GuestLine("ghost", "L", 1)
        )));

        assertThat(resp.items()).isEmpty();
        assertThat(resp.issues()).hasSize(1);
        assertThat(resp.issues().get(0).code()).isEqualTo("product_removed");
        assertThat(resp.issues().get(0).itemId()).isNull(); // cart-level toast
    }

    @Test
    @DisplayName("merge — non-ACTIVE product is skipped with product_removed issue")
    void mergeInactiveProduct() {
        Cart cart = emptyCart();
        Product retired = product("test-piece", "Test Piece", new BigDecimal("100000"), ProductStatus.ARCHIVED, 5);
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(productRepository.findBySlug("test-piece")).willReturn(Optional.of(retired));
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        CartResponse resp = service.merge(user, new MergeCartRequest(List.of(
                new GuestLine("test-piece", "L", 1)
        )));

        assertThat(resp.items()).isEmpty();
        assertThat(resp.issues())
                .extracting(i -> i.code()).containsExactly("product_removed");
    }

    @Test
    @DisplayName("merge — retired size is skipped with size_unavailable issue")
    void mergeSizeUnavailable() {
        Cart cart = emptyCart();
        Product product = product("test-piece", "Test Piece", new BigDecimal("100000"), ProductStatus.ACTIVE, 10);
        product.setAvailableSizes(Set.of("L")); // no XL any more
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(productRepository.findBySlug("test-piece")).willReturn(Optional.of(product));
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        CartResponse resp = service.merge(user, new MergeCartRequest(List.of(
                new GuestLine("test-piece", "XL", 1)
        )));

        assertThat(resp.items()).isEmpty();
        assertThat(resp.issues())
                .extracting(i -> i.code()).containsExactly("size_unavailable");
    }

    @Test
    @DisplayName("merge — cart already at cap; extra new lines dropped with cart_too_large (one issue for the whole overflow)")
    void mergeCartTooLarge() {
        Cart cart = emptyCart();
        for (int i = 0; i < 50; i++) {
            Product p = product("p-" + i, "P " + i, new BigDecimal("100"), ProductStatus.ACTIVE, 10);
            cart.getItems().add(item(cart, p, "L", 1, p.getPrice()));
        }
        Product overflow = product("overflow", "Overflow", new BigDecimal("100"), ProductStatus.ACTIVE, 10);

        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(productRepository.findBySlug("overflow")).willReturn(Optional.of(overflow));
        given(cartItemRepository.findByCartIdAndProductIdAndSize(any(), any(), any()))
                .willReturn(Optional.empty());
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        // Use only sizes that the default fixture actually offers ({L,XL})
        // so the cap check is what drops them — not size validation.
        // Three distinct (product, size) tuples via slug variance.
        Product overflow2 = product("overflow2", "Overflow 2", new BigDecimal("100"), ProductStatus.ACTIVE, 10);
        Product overflow3 = product("overflow3", "Overflow 3", new BigDecimal("100"), ProductStatus.ACTIVE, 10);
        given(productRepository.findBySlug("overflow2")).willReturn(Optional.of(overflow2));
        given(productRepository.findBySlug("overflow3")).willReturn(Optional.of(overflow3));

        CartResponse resp = service.merge(user, new MergeCartRequest(List.of(
                new GuestLine("overflow", "L", 1),
                new GuestLine("overflow2", "L", 1),
                new GuestLine("overflow3", "L", 1)
        )));

        // 50 pre-existing lines are unchanged; all 3 incoming were dropped;
        // a SINGLE cart_too_large issue accompanies (with count = 3).
        assertThat(resp.items()).hasSize(50);
        long tooLarge = resp.issues().stream().filter(i -> "cart_too_large".equals(i.code())).count();
        assertThat(tooLarge).isEqualTo(1);
        assertThat(resp.issues().stream()
                .filter(i -> "cart_too_large".equals(i.code()))
                .findFirst().orElseThrow().message()).contains("3 lines");
    }

    @Test
    @DisplayName("merge — mixed good and bad payload: good lines land, bad ones become issues")
    void mergeMixed() {
        Cart cart = emptyCart();
        Product good = product("good", "Good Piece", new BigDecimal("100"), ProductStatus.ACTIVE, 5);

        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(productRepository.findBySlug("good")).willReturn(Optional.of(good));
        given(productRepository.findBySlug("ghost")).willReturn(Optional.empty());
        given(cartItemRepository.findByCartIdAndProductIdAndSize(any(), any(), any()))
                .willReturn(Optional.empty());
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        CartResponse resp = service.merge(user, new MergeCartRequest(List.of(
                new GuestLine("good", "L", 2),
                new GuestLine("ghost", "L", 1)
        )));

        assertThat(resp.items()).hasSize(1);
        assertThat(resp.items().get(0).quantity()).isEqualTo(2);
        assertThat(resp.issues())
                .extracting(i -> i.code()).containsExactly("product_removed");
    }

    // ---------------------------------------------------------------------
    // Fixture helpers
    // ---------------------------------------------------------------------

    private Product product(String slug, String name, BigDecimal price, ProductStatus status, int stock) {
        Product p = new Product();
        p.setId(UUID.randomUUID());
        p.setSlug(slug);
        p.setName(name);
        p.setPrice(price);
        p.setCurrency("NGN");
        p.setStatus(status);
        p.setStockQuantity(stock);
        p.setAvailableSizes(Set.of("L", "XL"));
        return p;
    }

    private CartItem item(Cart cart, Product product, String size, int qty, BigDecimal snapshotPrice) {
        CartItem i = new CartItem();
        i.setId(UUID.randomUUID());
        i.setCart(cart);
        i.setProduct(product);
        i.setSize(size);
        i.setQuantity(qty);
        i.setSnapshotName(product.getName());
        i.setSnapshotSlug(product.getSlug());
        i.setSnapshotPrice(snapshotPrice);
        i.setSnapshotCurrency("NGN");
        i.setAddedAt(Instant.now());
        i.setUpdatedAt(Instant.now());
        return i;
    }
}
