package com.eazicut.api.products.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Wire shape for a product image — used both as inbound (create/update
 * requests) and outbound (product responses).
 *
 * <p>{@code id} is nullable on the wire: absent when a client submits a new
 * image, present when the server replies. The service layer decides whether
 * to treat a request-side {@code id} as "update this existing image" or to
 * ignore it.
 *
 * @param id        server-issued identifier (null on creation)
 * @param url       absolute image URL (validated non-blank on input)
 * @param alt       alternative text for accessibility (nullable)
 * @param sortOrder gallery ordering; lower values appear first
 * @param primary   whether this is the primary/cover shot
 */
public record ProductImageDto(
        UUID id,

        @NotBlank
        @Size(max = 2048)
        String url,

        @Size(max = 500)
        String alt,

        @Min(0)
        int sortOrder,

        boolean primary
) {
}
