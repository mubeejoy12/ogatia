package com.eazicut.api.categories.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Full wire shape for a {@code Category}.
 *
 * <p>Distinct from {@link CategorySummary}, which is the compact projection
 * embedded in {@code ProductResponse}. This response carries the description
 * and audit timestamps for the standalone {@code /api/v1/categories} reads.
 */
public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
