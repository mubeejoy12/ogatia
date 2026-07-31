package com.eazicut.api.cart.dto;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code POST /api/v1/cart/items}.
 *
 * <p>Add-or-increment semantics: if the caller's cart already has a line
 * for {@code (productId, size)}, the incoming {@code quantity} is added
 * to the existing one (capped at the per-line max). Otherwise a fresh
 * line is inserted with a snapshot of the product's current name / slug
 * / price / currency / image.
 *
 * <p>The service revalidates every field against the live product row
 * — a 200 here is proof the line landed correctly, not just proof the
 * request was well-formed.
 */
public record AddCartItemRequest(

        @NotNull
        UUID productId,

        @NotBlank
        @Size(max = 32)
        String size,

        @NotNull
        @Min(value = 1, message = "Quantity must be at least 1.")
        @Max(value = 20, message = "Quantity per line is capped at 20.")
        Integer quantity
) {
}
