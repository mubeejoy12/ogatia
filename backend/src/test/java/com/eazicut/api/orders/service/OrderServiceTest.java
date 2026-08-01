package com.eazicut.api.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

import com.eazicut.api.common.exception.ResourceNotFoundException;
import com.eazicut.api.orders.dto.OrderResponse;
import com.eazicut.api.orders.entity.Order;
import com.eazicut.api.orders.entity.OrderItem;
import com.eazicut.api.orders.entity.OrderStatus;
import com.eazicut.api.orders.entity.ShippingAddress;
import com.eazicut.api.orders.mapper.OrderMapper;
import com.eazicut.api.orders.mapper.OrderMapperImpl;
import com.eazicut.api.orders.repository.OrderRepository;
import com.eazicut.api.products.entity.Product;
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

    private final OrderMapper mapper = new OrderMapperImpl();

    private OrderService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new OrderService(orderRepository, mapper);

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
