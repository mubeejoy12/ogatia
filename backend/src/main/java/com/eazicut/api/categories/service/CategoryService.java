package com.eazicut.api.categories.service;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eazicut.api.categories.dto.CategoryRequest;
import com.eazicut.api.categories.dto.CategoryResponse;
import com.eazicut.api.categories.entity.Category;
import com.eazicut.api.categories.exception.CategoryInUseException;
import com.eazicut.api.categories.exception.DuplicateCategoryNameException;
import com.eazicut.api.categories.exception.DuplicateCategorySlugException;
import com.eazicut.api.categories.mapper.CategoryMapper;
import com.eazicut.api.categories.repository.CategoryRepository;
import com.eazicut.api.common.exception.ResourceNotFoundException;
import com.eazicut.api.products.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * Business logic for the Category feature.
 *
 * <p>Reads are {@code readOnly = true} for Hibernate dirty-check skip;
 * writes run inside the class-level {@link Transactional}.
 *
 * <p><strong>Name normalisation.</strong> Every incoming {@code name} is
 * trimmed and internal whitespace collapsed to single spaces via
 * {@link #normaliseName(String)} <em>before</em> the uniqueness probe or
 * the persist. That keeps stored values clean ("Suits" not "  Suits  ")
 * and guarantees {@code existsByNameIgnoreCase} sees the same shape the
 * insert will attempt.
 *
 * <p><strong>Uniqueness — two layers.</strong>
 * <ol>
 *   <li>Service probe ({@code existsBySlug}, {@code existsByNameIgnoreCase})
 *       — cheap, turns most conflicts into a clean 409 with a helpful
 *       message before any INSERT is attempted.</li>
 *   <li>DB constraint — {@code UNIQUE(slug)} plus a functional unique
 *       index on {@code LOWER(name)} (Stage 2 migration) — the final
 *       backstop for concurrent writes that slip past the probe.
 *       {@code DataIntegrityViolationException} is mapped to 409 by
 *       {@code GlobalExceptionHandler}.</li>
 * </ol>
 *
 * <p><strong>Delete safety.</strong> A category with products still
 * referencing it cannot be deleted; the service counts references first
 * and raises {@link CategoryInUseException} (409) with the exact count
 * so admin tooling can present a clear message instead of a generic
 * FK violation.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {

    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper mapper;

    // ---------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return mapper.toResponse(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", slug));
        return mapper.toResponse(category);
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> list(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(mapper::toResponse);
    }

    // ---------------------------------------------------------------------
    // Writes
    // ---------------------------------------------------------------------

    public CategoryResponse create(CategoryRequest request) {
        String name = normaliseName(request.name());
        assertSlugAvailable(request.slug());
        assertNameAvailable(name);

        Category category = mapper.toEntity(request);
        category.setName(name);

        Category saved = categoryRepository.save(category);
        return mapper.toResponse(saved);
    }

    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        String name = normaliseName(request.name());

        // Only re-probe uniqueness if the caller is changing the identifier
        // — probing on a no-op change would false-positive on the row itself.
        if (!category.getSlug().equals(request.slug())) {
            assertSlugAvailable(request.slug());
        }
        if (!category.getName().equalsIgnoreCase(name)) {
            assertNameAvailable(name);
        }

        mapper.updateEntity(request, category);
        category.setName(name);

        return mapper.toResponse(category);
    }

    /**
     * Hard delete — categories are reference data (see
     * {@code Category} entity comment). Refuses the delete if any product
     * still references this category so the FK never fires a bare
     * {@code DataIntegrityViolationException}.
     */
    public void delete(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        long inUse = productRepository.countByCategoryId(id);
        if (inUse > 0) {
            throw new CategoryInUseException(category.getSlug(), inUse);
        }

        categoryRepository.delete(category);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void assertSlugAvailable(String slug) {
        if (categoryRepository.existsBySlug(slug)) {
            throw new DuplicateCategorySlugException(slug);
        }
    }

    private void assertNameAvailable(String name) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateCategoryNameException(name);
        }
    }

    /**
     * Trim leading/trailing whitespace and collapse any run of internal
     * whitespace to a single space. Keeps stored names visually clean and
     * guarantees the uniqueness probe compares the same shape the INSERT
     * will attempt.
     */
    static String normaliseName(String name) {
        if (name == null) return null;
        return WHITESPACE_RUN.matcher(name.trim()).replaceAll(" ");
    }
}
