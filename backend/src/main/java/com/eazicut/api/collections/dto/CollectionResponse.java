package com.eazicut.api.collections.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Full wire shape for a {@code Collection}.
 *
 * <p>Distinct from {@link CollectionSummary}, which is the compact
 * projection embedded in {@code ProductResponse}. This response carries
 * the description and audit timestamps for the standalone
 * {@code /api/v1/collections} reads.
 */
public record CollectionResponse(
        UUID id,
        String name,
        String slug,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
