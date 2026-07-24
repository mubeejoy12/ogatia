package com.eazicut.api.categories.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create / update payload for a {@link com.eazicut.api.categories.entity.Category Category}.
 *
 * <p>PUT semantics on update — a full replacement of the mutable fields.
 * {@code description} is optional; {@code name} and {@code slug} are the
 * public identifiers and must always be present.
 *
 * <p>{@code name} is trimmed and whitespace-collapsed by the service before
 * persistence + uniqueness checks. Uniqueness on name is enforced
 * case-insensitively both at the service layer ({@code existsByNameIgnoreCase})
 * and at the database layer (functional unique index on {@code LOWER(name)},
 * introduced in the Stage 2 migration).
 *
 * <p>{@code slug} pattern matches the frontend's URL convention exactly — the
 * same regex as {@code ProductRequest}, so a category slug can be used
 * directly in {@code /shop?category=<slug>}.
 */
public record CategoryRequest(

        @NotBlank
        @Size(min = 2, max = 120)
        String name,

        @NotBlank
        @Size(min = 2, max = 140)
        @Pattern(
                regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "Slug must be lowercase words separated by single hyphens."
        )
        String slug,

        @Size(max = 2000)
        String description
) {
}
