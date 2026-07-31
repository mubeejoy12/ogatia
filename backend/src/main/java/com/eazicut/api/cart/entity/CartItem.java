package com.eazicut.api.cart.entity;

import java.math.BigDecimal;
import java.time.Instant;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.eazicut.api.products.entity.Product;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single line in a shopping cart. Uniquely identified by
 * {@code (cart_id, product_id, size)} — the same composite key the
 * pre-B005 localStorage cart used, so a customer can hold the same
 * piece in two sizes.
 *
 * <p><strong>Snapshot columns.</strong> {@code snapshotName},
 * {@code snapshotSlug}, {@code snapshotPrice}, {@code snapshotCurrency},
 * {@code snapshotImageUrl} capture the product's shape at the moment
 * of add. The cart UI renders from these fields — a mid-cart rename
 * or image swap never reshapes what the customer sees.
 *
 * <p><strong>CRITICAL — snapshot price is HISTORICAL / DISPLAY only.</strong>
 * {@link #snapshotPrice} is never used as the charged price on an
 * Order. The Order pipeline (later ticket) must:
 * <ol>
 *   <li>Compare {@code snapshotPrice} with the current
 *       {@code products.price}.</li>
 *   <li>If different, force the customer to explicitly confirm the new
 *       price before the Order is created.</li>
 *   <li>Persist the confirmed price into an {@code OrderItem} row —
 *       that becomes the immutable, chargeable amount.</li>
 * </ol>
 * A stale cart snapshot must NEVER silently become the final charged
 * price. B005's read surface surfaces price mismatches as
 * {@code CartIssue}s (later stage) so the UI can highlight them
 * before checkout.
 *
 * <p>Not soft-deleted. Removing a line hard-deletes the row; the
 * parent Cart's {@code orphanRemoval = true} handles that when the
 * item is popped from {@code cart.getItems()}.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_cart_item_line",
                        columnNames = {"cart_id", "product_id", "size"}
                )
        },
        indexes = {
                @Index(name = "idx_cart_item_product", columnList = "product_id")
        }
)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    /**
     * Live product reference. Products are soft-deleted so this FK is
     * never violated by a merchandising action; a manual hard delete
     * would fail loudly (safe outcome — see V6 header).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 32)
    private String size;

    @Column(nullable = false)
    private int quantity;

    // ---------- Snapshot: display-only, never a charged price ----------

    @Column(name = "snapshot_name", nullable = false, length = 200)
    private String snapshotName;

    @Column(name = "snapshot_slug", nullable = false, length = 220)
    private String snapshotSlug;

    @Column(name = "snapshot_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal snapshotPrice;

    @Column(name = "snapshot_currency", nullable = false, length = 3)
    private String snapshotCurrency;

    @Column(name = "snapshot_image_url", length = 2048)
    private String snapshotImageUrl;

    // ---------- Bespoke audit columns (added_at + updated_at) ----------
    //
    // Not @CreatedDate / @LastModifiedDate because AbstractAuditableEntity
    // is not extended here — cart items get a domain-specific added_at
    // that also drives the OrderBy on the parent Cart.

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    private void onCreate() {
        Instant now = Instant.now();
        if (addedAt == null) addedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }
}
