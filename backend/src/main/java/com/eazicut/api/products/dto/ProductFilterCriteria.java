package com.eazicut.api.products.dto;

import java.math.BigDecimal;

import com.eazicut.api.products.entity.ProductStatus;

/**
 * Query-parameter bind object for the paginated product list endpoint.
 *
 * <p>Every field is optional. Spring MVC binds the record from query
 * parameters via constructor binding (Spring 6+); pagination and sorting
 * are carried separately by a {@link org.springframework.data.domain.Pageable Pageable}
 * argument on the controller signature.
 *
 * <p>The specification builder (Stage 5) composes these into a JPA
 * {@code Specification<Product>}; any field left {@code null} contributes
 * no predicate.
 *
 * @param search     free-text search across name / short + full description / SKU
 * @param category   category slug (exact match)
 * @param collection collection slug (exact match)
 * @param minPrice   inclusive lower price bound
 * @param maxPrice   inclusive upper price bound
 * @param featured   restrict to featured products
 * @param newArrival restrict to newly-arrived products
 * @param bestseller restrict to bestsellers
 * @param available  restrict to products with stock available AND status ACTIVE
 * @param status     restrict to a specific {@link ProductStatus}
 */
public record ProductFilterCriteria(
        String search,
        String category,
        String collection,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean featured,
        Boolean newArrival,
        Boolean bestseller,
        Boolean available,
        ProductStatus status
) {

    // -----------------------------------------------------------------
    // Fluent construction helpers — avoid slotting ten nulls at the call
    // site when the caller only cares about one or two axes (typically
    // the convenience controller endpoints for featured / new-arrivals
    // / bestsellers).
    // -----------------------------------------------------------------

    public static ProductFilterCriteria empty() {
        return new ProductFilterCriteria(null, null, null, null, null, null, null, null, null, null);
    }

    public ProductFilterCriteria withFeatured(Boolean value) {
        return new ProductFilterCriteria(search, category, collection, minPrice, maxPrice,
                value, newArrival, bestseller, available, status);
    }

    public ProductFilterCriteria withNewArrival(Boolean value) {
        return new ProductFilterCriteria(search, category, collection, minPrice, maxPrice,
                featured, value, bestseller, available, status);
    }

    public ProductFilterCriteria withBestseller(Boolean value) {
        return new ProductFilterCriteria(search, category, collection, minPrice, maxPrice,
                featured, newArrival, value, available, status);
    }

    public ProductFilterCriteria withAvailable(Boolean value) {
        return new ProductFilterCriteria(search, category, collection, minPrice, maxPrice,
                featured, newArrival, bestseller, value, status);
    }
}
