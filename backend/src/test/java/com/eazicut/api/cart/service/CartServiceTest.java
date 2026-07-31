package com.eazicut.api.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
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

import com.eazicut.api.cart.dto.CartResponse;
import com.eazicut.api.cart.entity.Cart;
import com.eazicut.api.cart.entity.CartItem;
import com.eazicut.api.cart.mapper.CartMapper;
import com.eazicut.api.cart.mapper.CartMapperImpl;
import com.eazicut.api.cart.repository.CartRepository;
import com.eazicut.api.products.entity.Product;
import com.eazicut.api.products.entity.ProductStatus;
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

    private final CartMapper mapper = new CartMapperImpl();

    private CartService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new CartService(cartRepository, mapper);

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
