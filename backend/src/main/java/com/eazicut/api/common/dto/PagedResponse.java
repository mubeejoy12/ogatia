package com.eazicut.api.common.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Uniform pagination envelope for list endpoints.
 *
 * <p>Deliberately isolates the wire shape from Spring Data's
 * {@code PageImpl} — which serialises Spring internals and has
 * changed between minor versions. Every list endpoint constructs a
 * {@code PagedResponse} via {@link #from(Page)} or
 * {@link #from(Page, Function)} so the client contract stays stable.
 *
 * @param content       the page items
 * @param page          zero-based page index of this response
 * @param size          number of items requested per page
 * @param totalElements total items matching the query
 * @param totalPages    total pages for the current page size
 * @param first         true when this is the first page
 * @param last          true when this is the last page
 * @param empty         true when {@code content} is empty
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty
) {

    /**
     * Build directly from a {@link Page} whose items are already the desired
     * response type.
     */
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }

    /**
     * Build from a source page (usually entities) applying a per-item mapper
     * to obtain the response type. Convenient for {@code Page<Product>} →
     * {@code PagedResponse<ProductResponse>}.
     */
    public static <S, T> PagedResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return from(page.map(mapper));
    }
}
