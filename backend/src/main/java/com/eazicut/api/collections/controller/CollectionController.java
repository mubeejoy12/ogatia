package com.eazicut.api.collections.controller;

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

import com.eazicut.api.collections.dto.CollectionRequest;
import com.eazicut.api.collections.dto.CollectionResponse;
import com.eazicut.api.collections.service.CollectionService;
import com.eazicut.api.common.dto.ApiResponse;
import com.eazicut.api.common.dto.PagedResponse;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for the Collection feature.
 *
 * <p>Base path {@code /api/v1/collections}. Direct parallel of
 * {@code CategoryController} — same contract: {@link ApiResponse} for
 * singles, {@link PagedResponse} for lists, {@code hasRole('ADMIN')} on
 * writes, uniform {@code ApiError} body via the global exception handler.
 *
 * <p>Default list sort is {@code name ASC} — reference data reads best
 * alphabetically in admin tools and dropdowns.
 */
@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    // ---------------------------------------------------------------------
    // Reads (public)
    // ---------------------------------------------------------------------

    @GetMapping
    public PagedResponse<CollectionResponse> list(
            @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return PagedResponse.from(collectionService.list(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<CollectionResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(collectionService.getById(id));
    }

    @GetMapping("/slug/{slug}")
    public ApiResponse<CollectionResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.of(collectionService.getBySlug(slug));
    }

    // ---------------------------------------------------------------------
    // Writes (ADMIN)
    // ---------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CollectionResponse>> create(
            @Valid @RequestBody CollectionRequest request
    ) {
        CollectionResponse created = collectionService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.of(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CollectionResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CollectionRequest request
    ) {
        return ApiResponse.of(collectionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        collectionService.delete(id);
    }
}
