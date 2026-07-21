package com.eazicut.api.products.specification;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

import com.eazicut.api.products.dto.ProductFilterCriteria;
import com.eazicut.api.products.entity.Product;
import com.eazicut.api.products.entity.ProductStatus;

/**
 * Composable JPA {@link Specification} builders for {@link Product}.
 *
 * <p>Each builder returns {@code null} when its argument is absent —
 * {@link Specification#allOf} then contributes no predicate for that axis.
 * The result is a single dynamic WHERE clause tailored to whichever
 * filters the caller supplied.
 *
 * <p>Soft-delete is already enforced by the {@code @SQLRestriction} on
 * {@link Product}; no {@code notDeleted()} builder is required.
 *
 * <p>Slug-based filters ({@link #inCategory}, {@link #inCollection}) walk
 * the relationship using JPA's implicit join — Hibernate emits a single
 * SQL join, not a subquery.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
        // utility
    }

    // ---------------------------------------------------------------------
    // Composer
    // ---------------------------------------------------------------------

    /**
     * Compose the full filter for a paginated list request.
     *
     * <p>Each builder returns {@code null} for an absent axis; the varargs
     * form of {@link Specification#allOf(Specification[]) allOf} skips nulls
     * (whereas {@code List.of(...)} would throw NPE — do not switch back).
     */
    public static Specification<Product> matching(ProductFilterCriteria c) {
        if (c == null) {
            return Specification.allOf();
        }
        return Specification.allOf(
                hasKeyword(c.search()),
                inCategory(c.category()),
                inCollection(c.collection()),
                priceAtLeast(c.minPrice()),
                priceAtMost(c.maxPrice()),
                isFeatured(c.featured()),
                isNewArrival(c.newArrival()),
                isBestseller(c.bestseller()),
                isAvailable(c.available()),
                hasStatus(c.status())
        );
    }

    // ---------------------------------------------------------------------
    // Individual axes (public so tests can pin one behaviour at a time)
    // ---------------------------------------------------------------------

    public static Specification<Product> hasKeyword(String keyword) {
        if (isBlank(keyword)) return null;
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("shortDescription")), pattern),
                cb.like(cb.lower(root.get("fullDescription")), pattern),
                cb.like(cb.lower(root.get("sku")), pattern)
        );
    }

    public static Specification<Product> inCategory(String slug) {
        if (isBlank(slug)) return null;
        return (root, query, cb) -> cb.equal(root.get("category").get("slug"), slug);
    }

    public static Specification<Product> inCollection(String slug) {
        if (isBlank(slug)) return null;
        return (root, query, cb) -> cb.equal(root.get("collection").get("slug"), slug);
    }

    public static Specification<Product> priceAtLeast(BigDecimal min) {
        if (min == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Product> priceAtMost(BigDecimal max) {
        if (max == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }

    public static Specification<Product> isFeatured(Boolean value) {
        return booleanFlag("featured", value);
    }

    public static Specification<Product> isNewArrival(Boolean value) {
        return booleanFlag("newArrival", value);
    }

    public static Specification<Product> isBestseller(Boolean value) {
        return booleanFlag("bestseller", value);
    }

    /**
     * "Available" is a computed axis — active status AND positive stock.
     * A caller can pass {@code false} to explicitly show "unavailable"
     * products (staff dashboards); {@code null} means "no filter".
     */
    public static Specification<Product> isAvailable(Boolean value) {
        if (value == null) return null;
        return (root, query, cb) -> {
            if (value) {
                return cb.and(
                        cb.equal(root.get("status"), ProductStatus.ACTIVE),
                        cb.greaterThan(root.get("stockQuantity"), 0)
                );
            }
            return cb.or(
                    cb.notEqual(root.get("status"), ProductStatus.ACTIVE),
                    cb.equal(root.get("stockQuantity"), 0)
            );
        };
    }

    public static Specification<Product> hasStatus(ProductStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private static Specification<Product> booleanFlag(String field, Boolean value) {
        if (value == null) return null;
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
