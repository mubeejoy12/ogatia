package com.eazicut.api.products.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.eazicut.api.categories.entity.Category;
import com.eazicut.api.collections.entity.Collection;
import com.eazicut.api.common.entity.AbstractAuditableEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Product — the aggregate root of the merchandising domain.
 *
 * <p><strong>Money.</strong> Prices are always {@link BigDecimal} backed by a
 * {@code NUMERIC(19, 4)} column. Never widen to {@code double}: a floating-
 * point rounding error on a ₦850,000 tuxedo is an accounting incident.
 *
 * <p><strong>Soft delete.</strong> {@link org.hibernate.annotations.SQLDelete @SQLDelete}
 * rewrites the DELETE statement to an UPDATE of {@code deleted_at}; every read via
 * Hibernate is filtered by {@link org.hibernate.annotations.SQLRestriction @SQLRestriction}
 * so deleted rows are invisible to the ORM. Admin-facing "trash" queries — added later
 * — will use native SQL or the criteria API to bypass the restriction intentionally.
 *
 * <p><strong>Fetch strategy.</strong> Every relationship is {@code LAZY}; the read
 * paths that need eager fetching (PDP, cart line hydration) pull them in explicitly
 * via {@code @EntityGraph} on the repository query. This keeps the default page-listing
 * query cheap and predictable.
 *
 * <p><strong>Index rationale.</strong> Read-heavy columns get their own indexes so
 * common storefront queries (filter by category, by collection, by slug lookup, SKU
 * search) never scan the table. The unique constraints on {@code slug} and {@code sku}
 * are enforced at the DB level too — Bean Validation is not sufficient under
 * concurrent writes.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_slug", columnNames = "slug"),
                @UniqueConstraint(name = "uk_product_sku", columnNames = "sku")
        },
        indexes = {
                @Index(name = "idx_product_name", columnList = "name"),
                @Index(name = "idx_product_category_id", columnList = "category_id"),
                @Index(name = "idx_product_collection_id", columnList = "collection_id"),
                @Index(name = "idx_product_status", columnList = "status"),
                @Index(name = "idx_product_deleted_at", columnList = "deleted_at")
        }
)
@SQLDelete(sql = "UPDATE products SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Product extends AbstractAuditableEntity {

    // ---------------------------------------------------------------------
    // Identity
    // ---------------------------------------------------------------------

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    // ---------------------------------------------------------------------
    // Editorial + identification
    // ---------------------------------------------------------------------

    @Column(nullable = false, length = 200)
    @ToString.Include
    private String name;

    @Column(nullable = false, length = 220)
    @ToString.Include
    private String slug;

    @Column(name = "short_description", nullable = false, length = 500)
    private String shortDescription;

    @Column(name = "full_description", nullable = false, columnDefinition = "TEXT")
    private String fullDescription;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 100)
    private String brand = "Eazi Cut";

    // ---------------------------------------------------------------------
    // Money
    // ---------------------------------------------------------------------

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "discount_price", precision = 19, scale = 4)
    private BigDecimal discountPrice;

    /** ISO 4217. Naira for the launch market; nothing else is offered yet. */
    @Column(nullable = false, length = 3)
    private String currency = "NGN";

    // ---------------------------------------------------------------------
    // Lifecycle + merchandising flags
    // ---------------------------------------------------------------------

    /**
     * Persisted as plain {@code VARCHAR(20)} via {@link JdbcTypeCode}. Without
     * this override, Hibernate 6 uses a native ENUM type on H2 + PostgreSQL,
     * which drags PG's {@code CREATE TYPE} ceremony into every migration and
     * makes portable DDL awkward. VARCHAR is universal, greppable, and
     * evolvable without schema surgery.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity = 0;

    @Column(nullable = false)
    private boolean featured = false;

    @Column(name = "new_arrival", nullable = false)
    private boolean newArrival = false;

    @Column(nullable = false)
    private boolean bestseller = false;

    // ---------------------------------------------------------------------
    // Physical description
    // ---------------------------------------------------------------------

    @Column(name = "fabric_type", length = 100)
    private String fabricType;

    @Column(length = 60)
    private String color;

    // ---------------------------------------------------------------------
    // Collections — LinkedHashSet preserves insertion order for tags/sizes so
    // the merchandiser's input order is what appears on the storefront.
    // ---------------------------------------------------------------------

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "product_sizes",
            joinColumns = @JoinColumn(
                    name = "product_id",
                    foreignKey = @ForeignKey(name = "fk_product_sizes_product")
            )
    )
    @Column(name = "size", length = 32, nullable = false)
    @OrderBy
    @BatchSize(size = 25)
    private Set<String> availableSizes = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "product_tags",
            joinColumns = @JoinColumn(
                    name = "product_id",
                    foreignKey = @ForeignKey(name = "fk_product_tags_product")
            )
    )
    @Column(name = "tag", length = 64, nullable = false)
    @OrderBy
    @BatchSize(size = 25)
    private Set<String> tags = new LinkedHashSet<>();

    // ---------------------------------------------------------------------
    // Relationships
    // ---------------------------------------------------------------------

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 25)
    private List<ProductImage> images = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "category_id",
            foreignKey = @ForeignKey(name = "fk_product_category")
    )
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "collection_id",
            foreignKey = @ForeignKey(name = "fk_product_collection")
    )
    private Collection collection;

    // ---------------------------------------------------------------------
    // Soft delete
    // ---------------------------------------------------------------------

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // ---------------------------------------------------------------------
    // Association helpers — keep both sides of the OneToMany consistent
    // so callers don't have to remember which side to touch.
    // ---------------------------------------------------------------------

    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setProduct(null);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
