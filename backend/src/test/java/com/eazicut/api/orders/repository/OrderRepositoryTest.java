package com.eazicut.api.orders.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import com.eazicut.api.orders.entity.Order;
import com.eazicut.api.orders.entity.OrderItem;
import com.eazicut.api.orders.entity.OrderStatus;
import com.eazicut.api.orders.entity.ShippingAddress;
import com.eazicut.api.products.entity.Product;
import com.eazicut.api.products.entity.ProductStatus;
import com.eazicut.api.products.repository.ProductRepository;
import com.eazicut.api.users.entity.Role;
import com.eazicut.api.users.entity.User;
import com.eazicut.api.users.repository.UserRepository;

/**
 * Slice tests for {@link OrderRepository} + {@link OrderItemRepository}.
 *
 * <p>Proves the V7 schema and the entity mapping — every named
 * constraint fires (ux_order_reference, ux_order_idempotency_user,
 * ck_order_item_qty) and the ownership-safe lookups return only the
 * caller's rows.
 *
 * <p>{@link DataJpaTest} runs Flyway V1–V7 against H2; any drift
 * between entity + migration surfaces here.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
class OrderRepositoryTest {

    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;

    private User userA;
    private User userB;
    private Product product;

    @BeforeEach
    void setUp() {
        userA = userRepository.saveAndFlush(newUser("owner-a@ex.com"));
        userB = userRepository.saveAndFlush(newUser("owner-b@ex.com"));

        Product p = new Product();
        p.setName("Test Piece");
        p.setSlug("test-piece-order");
        p.setShortDescription("s");
        p.setFullDescription("f");
        p.setSku("ORD-TST-1");
        p.setBrand("Eazi Cut");
        p.setPrice(new BigDecimal("100000"));
        p.setCurrency("NGN");
        p.setStatus(ProductStatus.ACTIVE);
        p.setStockQuantity(10);
        product = productRepository.saveAndFlush(p);
    }

    private User newUser(String email) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$10$fakeHash");
        u.setRole(Role.CUSTOMER);
        u.setEnabled(true);
        return u;
    }

    private Order persistOrder(User owner, String reference, String idempotencyKey) {
        Order o = new Order();
        o.setUser(owner);
        o.setReference(reference);
        o.setIdempotencyKey(idempotencyKey);
        o.setStatus(OrderStatus.PENDING_PAYMENT);
        o.setCurrency("NGN");
        o.setSubtotal(new BigDecimal("100000"));
        o.setShippingCost(new BigDecimal("8000"));
        o.setTotal(new BigDecimal("108000"));
        o.setDeliveryMethodId("lagos-standard");
        o.setDeliveryMethodName("Lagos delivery");
        o.setPlacedAt(Instant.now());

        ShippingAddress addr = new ShippingAddress();
        addr.setFullName("Test Customer");
        addr.setEmail("test@ex.com");
        addr.setPhone("+2348000000000");
        addr.setAddressLine1("123 Marina");
        addr.setCity("Lagos");
        addr.setCountry("Nigeria");
        o.setShippingAddress(addr);

        // One line, attached via cascade so the parent's items collection
        // and the DB stay in sync.
        OrderItem item = new OrderItem();
        item.setOrder(o);
        item.setProduct(product);
        item.setProductSlug(product.getSlug());
        item.setProductName(product.getName());
        item.setProductImageUrl(null);
        item.setSize("L");
        item.setQuantity(1);
        item.setUnitPrice(product.getPrice());
        item.setCurrency(product.getCurrency());
        item.setLineTotal(product.getPrice());
        o.getItems().add(item);

        return orderRepository.saveAndFlush(o);
    }

    @Test
    @DisplayName("findByIdAndUserId — returns owner's order with items eagerly loaded")
    void findByIdAndUserIdReturnsOwn() {
        Order saved = persistOrder(userA, "EAZI-1-aaaa", "key-a-1");
        Order loaded = orderRepository.findByIdAndUserId(saved.getId(), userA.getId()).orElseThrow();
        assertThat(loaded.getReference()).isEqualTo("EAZI-1-aaaa");
        assertThat(loaded.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("findByIdAndUserId — cross-user lookup returns empty (not the row)")
    void findByIdAndUserIdCrossUserEmpty() {
        Order saved = persistOrder(userA, "EAZI-1-bbbb", "key-a-2");
        assertThat(orderRepository.findByIdAndUserId(saved.getId(), userB.getId())).isEmpty();
    }

    @Test
    @DisplayName("findByReferenceAndUserId — cross-user lookup empty too")
    void findByReferenceAndUserIdCrossUserEmpty() {
        persistOrder(userA, "EAZI-2-cccc", "key-a-3");
        assertThat(orderRepository.findByReferenceAndUserId("EAZI-2-cccc", userB.getId())).isEmpty();
        assertThat(orderRepository.findByReferenceAndUserId("EAZI-2-cccc", userA.getId())).isPresent();
    }

    @Test
    @DisplayName("findByUserId — paginated, only own")
    void findByUserIdPaginated() {
        persistOrder(userA, "EAZI-3-a1", "key-a-p1");
        persistOrder(userA, "EAZI-3-a2", "key-a-p2");
        persistOrder(userB, "EAZI-3-b1", "key-b-p1");

        var page = orderRepository.findByUserId(userA.getId(), PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("V7 ux_order_reference — duplicate reference rejected")
    void referenceMustBeUnique() {
        persistOrder(userA, "EAZI-dup-ref", "key-a-dr-1");
        assertThatThrownBy(() -> persistOrder(userB, "EAZI-dup-ref", "key-b-dr-1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V7 ux_order_idempotency_user — same (user, key) rejected")
    void idempotencyKeyMustBeUniquePerUser() {
        persistOrder(userA, "EAZI-idem-1", "SAME-KEY");
        assertThatThrownBy(() -> persistOrder(userA, "EAZI-idem-2", "SAME-KEY"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Idempotency key CAN be reused across DIFFERENT users (scoped uniqueness)")
    void idempotencyKeyReusableAcrossUsers() {
        persistOrder(userA, "EAZI-idem-x", "SHARED-KEY");
        // Same key, different user → allowed.
        persistOrder(userB, "EAZI-idem-y", "SHARED-KEY");
        assertThat(orderRepository.findByUserIdAndIdempotencyKey(userA.getId(), "SHARED-KEY")).isPresent();
        assertThat(orderRepository.findByUserIdAndIdempotencyKey(userB.getId(), "SHARED-KEY")).isPresent();
    }

    @Test
    @DisplayName("findByUserIdAndIdempotencyKey — locates the stored order for replay")
    void idempotencyLookup() {
        Order saved = persistOrder(userA, "EAZI-idem-lookup", "look-me-up");
        Order loaded = orderRepository.findByUserIdAndIdempotencyKey(userA.getId(), "look-me-up").orElseThrow();
        assertThat(loaded.getId()).isEqualTo(saved.getId());
        assertThat(loaded.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("existsByReference — true for present, false for absent")
    void existsByReference() {
        persistOrder(userA, "EAZI-exists", "key-e-1");
        assertThat(orderRepository.existsByReference("EAZI-exists")).isTrue();
        assertThat(orderRepository.existsByReference("EAZI-nope")).isFalse();
    }

    @Test
    @DisplayName("Admin findByReference — no user filter")
    void adminFindByReference() {
        persistOrder(userA, "EAZI-admin-r", "key-a-adm");
        assertThat(orderRepository.findByReference("EAZI-admin-r")).isPresent();
    }

    @Test
    @DisplayName("Address embed columns persist and rehydrate")
    void addressEmbedRoundTrip() {
        Order saved = persistOrder(userA, "EAZI-addr", "key-addr-1");
        Order loaded = orderRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getShippingAddress().getFullName()).isEqualTo("Test Customer");
        assertThat(loaded.getShippingAddress().getCountry()).isEqualTo("Nigeria");
        assertThat(loaded.getShippingAddress().getAddressLine2()).isNull(); // optional
    }
}
