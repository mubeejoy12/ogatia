package com.eazicut.api.collections.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.eazicut.api.collections.dto.CollectionRequest;
import com.eazicut.api.collections.dto.CollectionResponse;
import com.eazicut.api.collections.entity.Collection;

/**
 * MapStruct mapper for the Collection feature.
 *
 * <p>Same shape as {@code CategoryMapper}: pure scalar copies, no repository
 * access. Name normalisation lives in {@code CollectionService}; the
 * mapper stays intent-free.
 *
 * <p>{@link NullValuePropertyMappingStrategy#IGNORE} — a {@code null} on a
 * PUT never silently blanks a persisted column; combined with
 * {@code @NotBlank} on the request record, silent data loss on PUT is
 * impossible.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CollectionMapper {

    CollectionResponse toResponse(Collection collection);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Collection toEntity(CollectionRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CollectionRequest request, @MappingTarget Collection collection);
}
