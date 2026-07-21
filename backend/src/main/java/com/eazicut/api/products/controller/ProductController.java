package com.eazicut.api.products.controller;

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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.eazicut.api.common.dto.ApiResponse;
import com.eazicut.api.common.dto.PagedResponse;
import com.eazicut.api.products.dto.ProductFilterCriteria;
import com.eazicut.api.products.dto.ProductRequest;
import com.eazicut.api.products.dto.ProductResponse;
import com.eazicut.api.products.service.ProductService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for the Product feature.
 *
 * <p>Base path {@code /api/v1/products} — the {@code /api/v1} prefix comes
 * from the servlet context path, so {@code @RequestMapping("/products")} is
 * intentionally version-agnostic.
 *
 * <p><strong>Contract.</strong>
 * <ul>
 *   <li>Reads return {@link ApiResponse} envelopes for single items or
 *       {@link PagedResponse} for lists.</li>
 *   <li>Writes are guarded by {@link PreAuthorize @PreAuthorize} —
 *       {@code hasRole('ADMIN')} for POST/PUT/DELETE. In dev, an admin
 *       user is seeded via {@code spring.security.user.*} (HTTP Basic
 *       {@code admin}/{@code admin}). In prod, until real auth ships,
 *       writes return 403.</li>
 *   <li>All non-2xx responses flow through the global exception handler
 *       for a uniform {@code ApiError} body.</li>
 * </ul>
 *
 * <p><strong>Query parameters</strong> on {@link #list} — every field on
 * {@link ProductFilterCriteria} plus standard {@code page}, {@code size},
 * {@code sort} for {@link Pageable}. Max page size is capped in
 * {@code application.yml}; unknown sort properties return a 400 via the
 * global handler.
 */
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ---------------------------------------------------------------------
    // Reads (public)
    // ---------------------------------------------------------------------

    @GetMapping
    public PagedResponse<ProductResponse> list(
            @ModelAttribute ProductFilterCriteria criteria,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return PagedResponse.from(productService.list(criteria, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(productService.getById(id));
    }

    @GetMapping("/slug/{slug}")
    public ApiResponse<ProductResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.of(productService.getBySlug(slug));
    }

    // ---------------------------------------------------------------------
    // Writes (ADMIN)
    // ---------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody ProductRequest request
    ) {
        ProductResponse created = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.of(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request
    ) {
        return ApiResponse.of(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        productService.delete(id);
    }
}
