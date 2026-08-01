package com.eazicut.api.orders.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.eazicut.api.common.entity.AbstractAuditableEntity;
import com.eazicut.api.users.entity.User;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Order — the aggregate root of the commerce domain.
 *
 * <p><strong>Immutability.</strong> Once created (Stage 2), an order's
 * items, address, delivery method, and financial totals are
 * conceptually frozen. Only {@link #status} moves — through the
 * lifecycle defined in {@link OrderStatus}. B006 writes
 * {@code PENDING_PAYMENT} only; later tickets transition it.
 *
 * <p><strong>Reference.</strong> Human-facing identifier surfaced to
 * customers ("EAZI-1720617483471-A9F3C1"). Matches the pre-B006
 * localStorage prototype's shape. Unique at the DB level via
 * {@code ux_order_reference}; service regenerates on collision.
 *
 * <p><strong>Idempotency.</strong> {@link #idempotencyKey} is
 * required (D4). Uniqueness scoped to {@code (user_id, key)} via
 * {@code ux_order_idempotency_user}. Same key from the same user
 * replaying POST /orders returns the same order — see Stage 2.
 *
 * <p><strong>Snapshots.</strong> Delivery method id + name are
 * captured on the order so a later rename in {@code deliveryMethods.ts}
 * doesn't rewrite history. Item-level product snapshots live on
 * {@link OrderItem}.
 *
 * <p>Not soft-deleted. Compliance-sensitive records; retention is
 * "always" absent a data-erasure request.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_order_user",   columnList = "user_id"),
                @Index(name = "idx_order_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_order_reference",        columnNames = {"reference"}),
                @UniqueConstraint(name = "ux_order_idempotency_user", columnNames = {"user_id", "idempotency_key"})
        }
)
public class Order extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 40)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal;

    @Column(name = "shipping_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal shippingCost;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal total;

    // Denormalised delivery-method snapshot (see class Javadoc).
    @Column(name = "delivery_method_id", nullable = false, length = 64)
    private String deliveryMethodId;

    @Column(name = "delivery_method_name", nullable = false, length = 200)
    private String deliveryMethodName;

    @Embedded
    private ShippingAddress shippingAddress;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    /**
     * Order lines. Cascaded persist/merge/remove + orphanRemoval so
     * {@code order.getItems().add(item)} inside a create transaction
     * writes the child rows on flush.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 25)
    private List<OrderItem> items = new ArrayList<>();
}
