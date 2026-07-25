package com.eazicut.api.collections.service;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eazicut.api.collections.dto.CollectionRequest;
import com.eazicut.api.collections.dto.CollectionResponse;
import com.eazicut.api.collections.entity.Collection;
import com.eazicut.api.collections.exception.CollectionInUseException;
import com.eazicut.api.collections.exception.DuplicateCollectionNameException;
import com.eazicut.api.collections.exception.DuplicateCollectionSlugException;
import com.eazicut.api.collections.mapper.CollectionMapper;
import com.eazicut.api.collections.repository.CollectionRepository;
import com.eazicut.api.common.exception.ResourceNotFoundException;
import com.eazicut.api.products.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * Business logic for the Collection feature.
 *
 * <p>Direct parallel of {@code CategoryService}: same normalisation, same
 * two-layer uniqueness (service probe + DB unique index on
 * {@code name_lower}), same domain-level in-use guard on delete. See
 * {@code CategoryService} for the design rationale that applies to both.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CollectionService {

    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");

    private final CollectionRepository collectionRepository;
    private final ProductRepository productRepository;
    private final CollectionMapper mapper;

    // ---------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public CollectionResponse getById(UUID id) {
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", id));
        return mapper.toResponse(collection);
    }

    @Transactional(readOnly = true)
    public CollectionResponse getBySlug(String slug) {
        Collection collection = collectionRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", slug));
        return mapper.toResponse(collection);
    }

    @Transactional(readOnly = true)
    public Page<CollectionResponse> list(Pageable pageable) {
        return collectionRepository.findAll(pageable).map(mapper::toResponse);
    }

    // ---------------------------------------------------------------------
    // Writes
    // ---------------------------------------------------------------------

    public CollectionResponse create(CollectionRequest request) {
        String name = normaliseName(request.name());
        assertSlugAvailable(request.slug());
        assertNameAvailable(name);

        Collection collection = mapper.toEntity(request);
        collection.setName(name);

        Collection saved = collectionRepository.save(collection);
        return mapper.toResponse(saved);
    }

    public CollectionResponse update(UUID id, CollectionRequest request) {
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", id));

        String name = normaliseName(request.name());

        if (!collection.getSlug().equals(request.slug())) {
            assertSlugAvailable(request.slug());
        }
        if (!collection.getName().equalsIgnoreCase(name)) {
            assertNameAvailable(name);
        }

        mapper.updateEntity(request, collection);
        collection.setName(name);

        return mapper.toResponse(collection);
    }

    public void delete(UUID id) {
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", id));

        long inUse = productRepository.countByCollectionId(id);
        if (inUse > 0) {
            throw new CollectionInUseException(collection.getSlug(), inUse);
        }

        collectionRepository.delete(collection);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void assertSlugAvailable(String slug) {
        if (collectionRepository.existsBySlug(slug)) {
            throw new DuplicateCollectionSlugException(slug);
        }
    }

    private void assertNameAvailable(String name) {
        if (collectionRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateCollectionNameException(name);
        }
    }

    /**
     * Trim leading/trailing whitespace and collapse any run of internal
     * whitespace to a single space. Same shape as
     * {@code CategoryService.normaliseName}.
     */
    static String normaliseName(String name) {
        if (name == null) return null;
        return WHITESPACE_RUN.matcher(name.trim()).replaceAll(" ");
    }
}
