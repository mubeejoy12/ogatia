package com.eazicut.api.collections.dto;

import java.util.UUID;

/**
 * Compact collection projection used as a nested field on other resources
 * (currently {@code ProductResponse}).
 */
public record CollectionSummary(UUID id, String name, String slug) {
}
