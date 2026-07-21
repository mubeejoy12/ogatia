package com.eazicut.api.categories.dto;

import java.util.UUID;

/**
 * Compact category projection used as a nested field on other resources
 * (currently {@code ProductResponse}).
 *
 * <p>A future Category feature ticket may introduce a richer
 * {@code CategoryResponse}; this summary stays small on purpose so
 * nested payloads don't bloat.
 */
public record CategorySummary(UUID id, String name, String slug) {
}
