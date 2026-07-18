package com.eazicut.api.products.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.eazicut.api.categories.dto.CategorySummary;
import com.eazicut.api.collections.dto.CollectionSummary;
import com.eazicut.api.products.entity.ProductStatus;

/**
 * Wire shape for a {@code Product}. Never the entity — entities never leave
 * the service layer.
 *
 * <p>Nested Category / Collection references use the compact
 * {@link CategorySummary} / {@link CollectionSummary} projections so the
 * payload stays lean and future evolution of those modules doesn't ripple
 * into every product response.
 */
public record ProductResponse(
        UUID id,
        String name,
        String slug,
        String shortDescription,
        String fullDescription,
        String sku,
        String brand,
        BigDecimal price,
        BigDecimal discountPrice,
        String currency,
        ProductStatus status,
        int stockQuantity,
        boolean featured,
        boolean newArrival,
        boolean bestseller,
        String fabricType,
        String color,
        List<String> availableSizes,
        List<String> tags,
        List<ProductImageDto> images,
        CategorySummary category,
        CollectionSummary collection,
        Instant createdAt,
        Instant updatedAt
) {
}
