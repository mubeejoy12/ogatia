package com.eazicut.api.categories.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.eazicut.api.categories.dto.CategoryRequest;
import com.eazicut.api.categories.dto.CategoryResponse;
import com.eazicut.api.categories.entity.Category;

/**
 * MapStruct mapper for the Category feature.
 *
 * <p>Same shape as {@code ProductMapper}: pure scalar copies, no repository
 * access. Normalisation of the {@code name} field (trim + collapse
 * whitespace) is done in the service, not here — the mapper stays
 * intent-free.
 *
 * <p>{@link NullValuePropertyMappingStrategy#IGNORE} means a {@code null}
 * value on a request update never silently blanks an existing column;
 * combined with the {@code @NotBlank} validation on the request DTO, this
 * makes accidental data loss on PUT effectively impossible.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CategoryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CategoryRequest request, @MappingTarget Category category);
}
