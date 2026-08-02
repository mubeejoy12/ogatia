package com.eazicut.api.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.eazicut.api.cart.entity.Cart;
import com.eazicut.api.cart.entity.CartItem;
import com.eazicut.api.cart.exception.InsufficientStockException;
import com.eazicut.api.cart.exception.ProductUnavailableException;
import com.eazicut.api.cart.exception.SizeUnavailableException;
import com.eazicut.api.cart.repository.CartRepository;
import com.eazicut.api.common.exception.ResourceNotFoundException;
import com.eazicut.api.orders.dto.CreateOrderRequest;
import com.eazicut.api.orders.dto.CreateShippingAddressRequest;
import com.eazicut.api.orders.dto.OrderResponse;
import com.eazicut.api.orders.entity.Order;
import com.eazicut.api.orders.entity.OrderItem;
import com.eazicut.api.orders.entity.OrderStatus;
import com.eazicut.api.orders.entity.ShippingAddress;
import com.eazicut.api.orders.exception.CartEmptyException;
import com.eazicut.api.orders.exception.MissingIdempotencyKeyException;
import com.eazicut.api.orders.exception.PriceMismatchException;
import com.eazicut.api.orders.exception.UnknownDeliveryMethodException;
import com.eazicut.api.orders.mapper.OrderMapper;
import com.eazicut.api.orders.mapper.OrderMapperImpl;
import com.eazicut.api.orders.repository.OrderRepository;
import com.eazicut.api.products.entity.Product;
import com.eazicut.api.products.entity.ProductStatus;
import com.eazicut.api.users.entity.Role;
import com.eazicut.api.users.entity.User;

/**
 * Unit tests for {@link OrderService} — Stage 1 read-only surface.
 *
 * <p>Uses the generated {@link OrderMapperImpl} directly (MapStruct
 * emits a plain class with no Spring dependencies) so mapping stays
 * exercised alongside the service logic; the repository is mocked.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;

    private final OrderMapper mapper = new OrderMapperImpl();
    private final DeliveryMethodCatalog catalog = new DeliveryMethodCatalog();
    // Real generator, real repo mock — collision path exercised by the
    // fact that existsByReference returns false (Mockito default).
    private OrderNumberGenerator generator;

    private OrderService service;
    private User user;

    @BeforeEach
    void setUp() {
        generator = new OrderNumberGenerator(orderRepository);
        service = new OrderService(orderRepository, mapper, cartRepository, catalog, generator);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("customer@example.com");
        user.setEmailLower("customer@example.com");
        user.setPasswordHash("$2a$10$fake");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
    }

    // ------------------------------------------------------------------
    // Customer-facing
    // ------------------------------------------------------------------

    @Test
    @DisplayName("findByIdForUser — returns the caller's order")
    void findByIdForUserHappy() {
        Order order = sampleOrder("EAZI-x-1");
        given(orderRepository.findByIdAndUserId(order.getId(), user.getId()))
                .willReturn(Optional.of(order));

        OrderResponse resp = service.findByIdForUser(user, order.getId());

        assertThat(resp.reference()).isEqualTo("EAZI-x-1");
        assertThat(resp.items()).hasSize(1);
        assertThat(resp.total()).isEqualByComparingTo(order.getTotal());
    }

    @Test
    @DisplayName("findByIdForUser — cross-user / unknown id → 404 (identical shape)")
    void findByIdForUserMissing() {
        UUID id = UUID.randomUUID();
        given(orderRepository.findByIdAndUserId(id, user.getId())).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.findByIdForUser(user, id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order");
    }

    @Test
    @DisplayName("findByReferenceForUser — returns the caller's order")
    void findByReferenceForUserHappy() {
        Order order = sampleOrder("EAZI-ref-1");
        given(orderRepository.findByReferenceAndUserId("EAZI-ref-1", user.getId()))
                .willReturn(Optional.of(order));

        OrderResponse resp = service.findByReferenceForUser(user, "EAZI-ref-1");
        assertThat(resp.reference()).isEqualTo("EAZI-ref-1");
    }

    @Test
    @DisplayName("findByReferenceForUser — cross-user / unknown ref → 404")
    void findByReferenceForUserMissing() {
        given(orderRepository.findByReferenceAndUserId("EAZI-nope", user.getId()))
                .willReturn(Optional.empty());
        assertThatThrownBy(() -> service.findByReferenceForUser(user, "EAZI-nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("findForUser — paginated projection")
    void findForUserPaged() {
        Order o1 = sampleOrder("EAZI-p-1");
        Order o2 = sampleOrder("EAZI-p-2");
        Page<Order> page = new PageImpl<>(List.of(o1, o2), PageRequest.of(0, 10), 2);
        given(orderRepository.findByUserId(user.getId(), PageRequest.of(0, 10)))
                .willReturn(page);

        Page<OrderResponse> resp = service.findForUser(user, PageRequest.of(0, 10));
        assertThat(resp.getTotalElements()).isEqualTo(2);
        assertThat(resp.getContent())
                .extracting(OrderResponse::reference)
                .containsExactly("EAZI-p-1", "EAZI-p-2");
    }

    // ------------------------------------------------------------------
    // Admin-facing
    // ------------------------------------------------------------------

    @Test
    @DisplayName("adminFindById — no user filter; returns any order")
    void adminFindById() {
        Order order = sampleOrder("EAZI-admin-1");
        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));

        OrderResponse resp = service.adminFindById(order.getId());
        assertThat(resp.reference()).isEqualTo("EAZI-admin-1");
    }

    @Test
    @DisplayName("adminFindById — unknown id → 404")
    void adminFindByIdMissing() {
        UUID id = UUID.randomUUID();
        given(orderRepository.findById(id)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.adminFindById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("adminFindByReference — no user filter; returns any order")
    void adminFindByReference() {
        Order order = sampleOrder("EAZI-admin-ref");
        given(orderRepository.findByReference("EAZI-admin-ref")).willReturn(Optional.of(order));

        OrderResponse resp = service.adminFindByReference("EAZI-admin-ref");
        assertThat(resp.reference()).isEqualTo("EAZI-admin-ref");
    }

    @Test
    @DisplayName("adminFindAll — paginated across all users")
    void adminFindAll() {
        Order o1 = sampleOrder("EAZI-a-1");
        Order o2 = sampleOrder("EAZI-a-2");
        given(orderRepository.findAll(PageRequest.of(0, 10)))
                .willReturn(new PageImpl<>(List.of(o1, o2), PageRequest.of(0, 10), 2));

        Page<OrderResponse> resp = service.adminFindAll(PageRequest.of(0, 10));
        assertThat(resp.getTotalElements()).isEqualTo(2);
    }

    // ------------------------------------------------------------------
    // createFromCart (Stage 2)
    // ------------------------------------------------------------------

    private static final CreateShippingAddressRequest VALID_ADDRESS = new CreateShippingAddressRequest(
            "Test Customer", "test@ex.com", "+2348000000000",
            "123 Marina", null, "Lagos", null, null, "Nigeria");

    private Product activeProduct(String slug, BigDecimal price, int stock) {
        Product p = new Product();
        p.setId(UUID.randomUUID());
        p.setSlug(slug);
        p.setName("Piece " + slug);
        p.setPrice(price);
        p.setCurrency("NGN");
        p.setStatus(ProductStatus.ACTIVE);
        p.setStockQuantity(stock);
        p.setAvailableSizes(Set.of("L", "XL"));
        return p;
    }

    private CartItem cartLine(Cart cart, Product product, String size, int qty, BigDecimal snapshotPrice) {
        CartItem i = new CartItem();
        i.setId(UUID.randomUUID());
        i.setCart(cart);
        i.setProduct(product);
        i.setSize(size);
        i.setQuantity(qty);
        i.setSnapshotSlug(product.getSlug());
        i.setSnapshotName(product.getName());
        i.setSnapshotPrice(snapshotPrice);
        i.setSnapshotCurrency("NGN");
        i.setSnapshotImageUrl("https://example.com/x.jpg");
        i.setAddedAt(Instant.now());
        i.setUpdatedAt(Instant.now());
        return i;
    }

    private Cart cartWith(CartItem... items) {
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUser(user);
        cart.setCurrency("NGN");
        cart.setUpdatedAt(Instant.now());
        cart.setItems(new ArrayList<>(List.of(items)));
        return cart;
    }

    @Test
    @DisplayName("create — missing Idempotency-Key → MissingIdempotencyKeyException (400)")
    void createMissingKey() {
        var req = new CreateOrderRequest("lagos-standard", VALID_ADDRESS, new BigDecimal("108000"));
        assertThatThrownBy(() -> service.createFromCart(user, req, null))
                .isInstanceOf(MissingIdempotencyKeyException.class);
        assertThatThrownBy(() -> service.createFromCart(user, req, "   "))
                .isInstanceOf(MissingIdempotencyKeyException.class);
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("create — idempotent replay returns the stored order verbatim, no new write")
    void createIdempotentReplay() {
        var req = new CreateOrderRequest("lagos-standard", VALID_ADDRESS, new BigDecimal("108000"));
        Order stored = sampleOrder("EAZI-existing");
        given(orderRepository.findByUserIdAndIdempotencyKey(user.getId(), "key-abc"))
                .willReturn(Optional.of(stored));

        OrderResponse resp = service.createFromCart(user, req, "key-abc");

        assertThat(resp.reference()).isEqualTo("EAZI-existing");
        verify(orderRepository, never()).saveAndFlush(any());
        verify(cartRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("create — empty cart → CartEmptyException (400)")
    void createCartEmpty() {
        var req = new CreateOrderRequest("lagos-standard", VALID_ADDRESS, new BigDecimal("8000"));
        given(orderRepository.findByUserIdAndIdempotencyKey(user.getId(), "key")).willReturn(Optional.empty());
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cartWith()));

        assertThatThrownBy(() -> service.createFromCart(user, req, "key"))
                .isInstanceOf(CartEmptyException.class);
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("create — no cart row for the user → CartEmptyException")
    void createNoCart() {
        var req = new CreateOrderRequest("lagos-standard", VALID_ADDRESS, new BigDecimal("8000"));
        given(orderRepository.findByUserIdAndIdempotencyKey(user.getId(), "key")).willReturn(Optional.empty());
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createFromCart(user, req, "key"))
                .isInstanceOf(CartEmptyException.class);
    }

    @Test
    @DisplayName("create — unknown delivery method → UnknownDeliveryMethodException (400)")
    void createUnknownDeliveryMethod() {
        Product p = activeProduct("alpha", new BigDecimal("100000"), 5);
        Cart cart = cartWith(cartLine(null, p, "L", 1, p.getPrice()));
        given(orderRepository.findByUserIdAndIdempotencyKey(user.getId(), "key")).willReturn(Optional.empty());
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));

        var req = new CreateOrderRequest("nonexistent", VALID_ADDRESS, new BigDecimal("100000"));
        assertThatThrownBy(() -> service.createFromCart(user, req, "key"))
                .isInstanceOf(UnknownDeliveryMethodException.class)
                .hasMessageContaining("nonexistent");
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("create — line references non-ACTIVE product → ProductUnavailableException (409)")
    void createProductUnavailable() {
        Product p = activeProduct("alpha", new BigDecimal("100000"), 5);
        p.setStatus(ProductStatus.OUT_OF_STOCK);
        Cart cart = cartWith(cartLine(null, p, "L", 1, p.getPrice()));
        given(orderRepository.findByUserIdAndIdempotencyKey(user.getId(), "key")).willReturn(Optional.empty());
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));

        var req = new CreateOrderRequest("lagos-standard", VALID_ADDRESS, new BigDecimal("108000"));
        assertThatThrownBy(() -> service.createFromCart(user, req, "key"))
                .isInstanceOf(ProductUnavailableException.class);
    }

    @Test
    @DisplayName("create — line size no longer offered → SizeUnavailableException (409)")
    void createSizeUnavailable() {
        Product p = activeProduct("alpha", new BigDecimal("100000"), 5);
        p.setAvailableSizes(Set.of("XL")); // no L any more
        Cart cart = cartWith(cartLine(null, p, "L", 1, p.getPrice()));
        given(orderRepository.findByUserIdAndIdempotencyKey(user.getId(), "key")).willReturn(Optional.empty());
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));

        var req = new CreateOrderRequest("lagos-standard", VALID_ADDRESS, new BigDecimal("108000"));
        assertThatThrownBy(() -> service.createFromCart(user, req, "key"))
                .isInstanceOf(SizeUnavailableException.class);
    }

    @Test
    @DisplayName("create — stock below quantity → InsufficientStockException (409)")
    void createInsufficientStock() {
        Product p = activeProduct("alpha", new BigDecimal("100000"), 1);
        Cart cart = cartWith(cartLine(null, p, "L", 3, p.getPrice()));
        given(orderRepository.findByUserIdAndIdempotencyKey(user.getId(), "key")).willReturn(Optional.empty());
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));

        var req = new CreateOrderRequest("lagos-standard", VALID_ADDRESS, new BigDecimal("308000"));
        assertThatThrownBy(() -> service.createFromCart(user, req, "key"))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("create — expectedTotal disagrees with live recompute → PriceMismatchException (409) carrying current total")
    void createPriceMismatch() {
        // Product now costs 130k, cart snapshot said 100k. Customer submitted
        // expectedTotal 108k (100k + 8k shipping). Server refuses.
        Product p = activeProduct("alpha", new BigDecimal("130000"), 5);
        Cart cart = cartWith(cartLine(null, p, "L", 1, new BigDecimal("100000")));
        given(orderRepository.findByUserIdAndIdempotencyKey(user.getId(), "key")).willReturn(Optional.empty());
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));

        var req = new CreateOrderRequest("lagos-standard", VALID_ADDRESS, new BigDecimal("108000"));
        assertThatThrownBy(() -> service.createFromCart(user, req, "key"))
                .isInstanceOfSatisfying(PriceMismatchException.class, ex -> {
                    // 130k + 8k = 138k current total
                    assertThat(ex.currentTotal()).isEqualByComparingTo(new BigDecimal("138000"));
                });
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("create — happy path: writes Order with LIVE prices (not snapshot), clears cart, returns response")
    void createHappy() {
        // Snapshot said 100k; product now costs 130k; customer's expectedTotal
        // is 138k (they've re-reviewed post price change). Order lands with
        // LIVE 130k in unitPrice.
        Product p = activeProduct("alpha", new BigDecimal("130000"), 5);
        Cart cart = cartWith(cartLine(null, p, "L", 1, new BigDecimal("100000")));

        given(orderRepository.findByUserIdAndIdempotencyKey(user.getId(), "key")).willReturn(Optional.empty());
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(orderRepository.existsByReference(anyString())).willReturn(false);
        given(orderRepository.saveAndFlush(any(Order.class))).willAnswer(inv -> inv.getArgument(0));
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        var req = new CreateOrderRequest("lagos-standard", VALID_ADDRESS, new BigDecimal("138000"));
        OrderResponse resp = service.createFromCart(user, req, "key");

        // Reference is generated in EAZI-<epoch>-<hex4> shape
        assertThat(resp.reference()).matches("^EAZI-\\d+-[0-9A-F]{4}$");
        assertThat(resp.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(resp.currency()).isEqualTo("NGN");
        assertThat(resp.subtotal()).isEqualByComparingTo(new BigDecimal("130000"));
        assertThat(resp.shippingCost()).isEqualByComparingTo(new BigDecimal("8000"));
        assertThat(resp.total()).isEqualByComparingTo(new BigDecimal("138000"));
        assertThat(resp.deliveryMethodId()).isEqualTo("lagos-standard");
        assertThat(resp.deliveryMethodName()).isEqualTo("Lagos delivery");
        assertThat(resp.items()).hasSize(1);
        // LIVE 130k, not the 100k snapshot from the cart
        assertThat(resp.items().get(0).unitPrice()).isEqualByComparingTo(new BigDecimal("130000"));
        assertThat(resp.items().get(0).lineTotal()).isEqualByComparingTo(new BigDecimal("130000"));

        // Cart was cleared (verified via the saveAndFlush arg)
        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).saveAndFlush(cart);
    }

    @Test
    @DisplayName("create — reference generator retries on collision; first hit resolves")
    void createReferenceCollisionRetry() {
        Product p = activeProduct("alpha", new BigDecimal("100000"), 5);
        Cart cart = cartWith(cartLine(null, p, "L", 1, p.getPrice()));

        given(orderRepository.findByUserIdAndIdempotencyKey(user.getId(), "key")).willReturn(Optional.empty());
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        // First reference clashes, second is free.
        given(orderRepository.existsByReference(anyString()))
                .willReturn(true, false);
        given(orderRepository.saveAndFlush(any(Order.class))).willAnswer(inv -> inv.getArgument(0));
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        var req = new CreateOrderRequest("lagos-standard", VALID_ADDRESS, new BigDecimal("108000"));
        OrderResponse resp = service.createFromCart(user, req, "key");
        assertThat(resp.reference()).matches("^EAZI-\\d+-[0-9A-F]{4}$");
    }

    @Test
    @DisplayName("create — atelier-pickup shipping cost is 0; expectedTotal must match subtotal exactly")
    void createFreeShipping() {
        Product p = activeProduct("alpha", new BigDecimal("100000"), 5);
        Cart cart = cartWith(cartLine(null, p, "L", 1, p.getPrice()));

        given(orderRepository.findByUserIdAndIdempotencyKey(user.getId(), "key")).willReturn(Optional.empty());
        given(cartRepository.findByUserId(user.getId())).willReturn(Optional.of(cart));
        given(orderRepository.existsByReference(anyString())).willReturn(false);
        given(orderRepository.saveAndFlush(any(Order.class))).willAnswer(inv -> inv.getArgument(0));
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(inv -> inv.getArgument(0));

        var req = new CreateOrderRequest("atelier-pickup", VALID_ADDRESS, new BigDecimal("100000"));
        OrderResponse resp = service.createFromCart(user, req, "key");

        assertThat(resp.shippingCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.total()).isEqualByComparingTo(new BigDecimal("100000"));
    }

    // ------------------------------------------------------------------
    // Fixture helper
    // ------------------------------------------------------------------

    private Order sampleOrder(String reference) {
        Order o = new Order();
        o.setId(UUID.randomUUID());
        o.setUser(user);
        o.setReference(reference);
        o.setIdempotencyKey("k-" + reference);
        o.setStatus(OrderStatus.PENDING_PAYMENT);
        o.setCurrency("NGN");
        o.setSubtotal(new BigDecimal("100000"));
        o.setShippingCost(new BigDecimal("8000"));
        o.setTotal(new BigDecimal("108000"));
        o.setDeliveryMethodId("lagos-standard");
        o.setDeliveryMethodName("Lagos delivery");
        o.setPlacedAt(Instant.now());
        o.setUpdatedAt(Instant.now());

        ShippingAddress a = new ShippingAddress();
        a.setFullName("Test Customer");
        a.setEmail("test@ex.com");
        a.setPhone("+2348000000000");
        a.setAddressLine1("123 Marina");
        a.setCity("Lagos");
        a.setCountry("Nigeria");
        o.setShippingAddress(a);

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setSlug("test-piece");
        product.setName("Test Piece");
        product.setPrice(new BigDecimal("100000"));
        product.setCurrency("NGN");

        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setOrder(o);
        item.setProduct(product);
        item.setProductSlug(product.getSlug());
        item.setProductName(product.getName());
        item.setSize("L");
        item.setQuantity(1);
        item.setUnitPrice(product.getPrice());
        item.setCurrency("NGN");
        item.setLineTotal(product.getPrice());

        o.setItems(new ArrayList<>(List.of(item)));
        return o;
    }
}
