package com.eazicut.api.collections.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create / update payload for a
 * {@link com.eazicut.api.collections.entity.Collection Collection}.
 *
 * <p>Same shape as {@link com.eazicut.api.categories.dto.CategoryRequest CategoryRequest}
 * — see that class for the rationale behind the name / slug rules and the
 * two-layer uniqueness enforcement. Collections are the storefront's
 * seasonal / thematic groupings ("The Onyx Bespoke", "Ivory Wedding");
 * validation rules are identical to Category by design.
 */
public record CollectionRequest(

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
