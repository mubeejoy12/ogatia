package com.eazicut.api.products.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.eazicut.api.categories.dto.CategorySummary;
import com.eazicut.api.categories.entity.Category;
import com.eazicut.api.collections.dto.CollectionSummary;
import com.eazicut.api.collections.entity.Collection;
import com.eazicut.api.products.dto.ProductImageDto;
import com.eazicut.api.products.dto.ProductRequest;
import com.eazicut.api.products.dto.ProductResponse;
import com.eazicut.api.products.entity.Product;
import com.eazicut.api.products.entity.ProductImage;

/**
 * MapStruct mapper for the Product feature.
 *
 * <p>Category / Collection resolution lives in the service, not here — the
 * mapper deliberately has no repository access. It handles scalar copies,
 * nested {@link CategorySummary} / {@link CollectionSummary} projections,
 * and image conversion.
 *
 * <p>For creation, {@link #toEntity(ProductRequest)} leaves id, timestamps,
 * relationships and images unset — the service performs those steps so the
 * bidirectional back-refs on the image collection stay consistent.
 *
 * <p>For update, {@link #updateEntity(ProductRequest, Product)} merges the
 * request into an existing entity with the same ignore list, respecting
 * {@link NullValuePropertyMappingStrategy#IGNORE} so a {@code null} field
 * on the request never nulls out an existing value silently. Full-replacement
 * fields the service actively wants overwritten are set explicitly by the
 * service using its own defaults.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProductMapper {

    // ---------------------------------------------------------------------
    // Response mapping
    // ---------------------------------------------------------------------

    ProductResponse toResponse(Product product);

    ProductImageDto toImageDto(ProductImage image);

    CategorySummary toCategorySummary(Category category);

    CollectionSummary toCollectionSummary(Collection collection);

    // ---------------------------------------------------------------------
    // Request → new entity
    // ---------------------------------------------------------------------

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "collection", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Product toEntity(ProductRequest request);

    // ---------------------------------------------------------------------
    // Request → existing entity (merge for update)
    // ---------------------------------------------------------------------

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "collection", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(ProductRequest request, @MappingTarget Product product);

    // ---------------------------------------------------------------------
    // Image DTO → entity (used by the service when materialising the image
    // collection; back-ref is set by Product.addImage)
    // ---------------------------------------------------------------------

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductImage toImageEntity(ProductImageDto dto);
}
