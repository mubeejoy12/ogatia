package com.eazicut.api.orders.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.eazicut.api.common.entity.AbstractAuditableEntity;
import com.eazicut.api.products.entity.Product;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single line on an {@link Order}.
 *
 * <p><strong>The immutable charge record.</strong> Once written, this
 * row is the legal / accounting truth of what the customer bought.
 * {@link #unitPrice} is the price the customer confirmed at
 * order-creation time — never the cart snapshot, never a stale
 * catalogue value. Bakes the B005 pricing invariant: cart snapshot
 * is display-only, {@code OrderItem.unitPrice} is chargeable.
 *
 * <p>Product identity is captured in two forms:
 *
 * <ul>
 *   <li>{@link #product} FK — links to the live product row for
 *       admin drill-down and re-order support. Non-cascading; products
 *       are soft-deleted so the row never physically vanishes.</li>
 *   <li>{@code productSlug} / {@code productName} /
 *       {@code productImageUrl} snapshots — what the customer saw
 *       + what refunds and receipts must show even if the product
 *       is later renamed or re-photographed.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_item_order",   columnList = "order_id"),
                @Index(name = "idx_order_item_product", columnList = "product_id")
        }
)
public class OrderItem extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // --------- Product identity snapshots ---------

    @Column(name = "product_slug", nullable = false, length = 220)
    private String productSlug;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "product_image_url", length = 2048)
    private String productImageUrl;

    // --------- Line data ---------

    @Column(nullable = false, length = 32)
    private String size;

    @Column(nullable = false)
    private int quantity;

    /**
     * The price the customer confirmed at order-creation time. Never
     * a cart snapshot, never a live catalogue value. Immutable once
     * written.
     */
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTotal;
}
