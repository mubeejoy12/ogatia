package com.eazicut.api.categories.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.eazicut.api.categories.dto.CategoryRequest;
import com.eazicut.api.categories.dto.CategoryResponse;
import com.eazicut.api.categories.service.CategoryService;
import com.eazicut.api.common.dto.ApiResponse;
import com.eazicut.api.common.dto.PagedResponse;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for the Category feature.
 *
 * <p>Base path {@code /api/v1/categories}. Contract mirrors
 * {@code ProductController}:
 * <ul>
 *   <li>Reads return {@link ApiResponse} envelopes for single items or
 *       {@link PagedResponse} for lists.</li>
 *   <li>Writes are guarded by {@link PreAuthorize @PreAuthorize} —
 *       {@code hasRole('ADMIN')} for POST/PUT/DELETE.</li>
 *   <li>All non-2xx responses flow through the global exception handler
 *       for a uniform {@code ApiError} body.</li>
 * </ul>
 *
 * <p>Default list sort is {@code name ASC} — reference data reads best
 * alphabetically in admin tools and dropdowns.
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // ---------------------------------------------------------------------
    // Reads (public)
    // ---------------------------------------------------------------------

    @GetMapping
    public PagedResponse<CategoryResponse> list(
            @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return PagedResponse.from(categoryService.list(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(categoryService.getById(id));
    }

    @GetMapping("/slug/{slug}")
    public ApiResponse<CategoryResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.of(categoryService.getBySlug(slug));
    }

    // ---------------------------------------------------------------------
    // Writes (ADMIN)
    // ---------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CategoryRequest request
    ) {
        CategoryResponse created = categoryService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.of(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request
    ) {
        return ApiResponse.of(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        categoryService.delete(id);
    }
}
