package com.eazicut.api.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for {@code PATCH /api/v1/cart/items/{itemId}}.
 *
 * <p>Sets the absolute quantity for a line (not a delta). A quantity of
 * {@code 0} is not accepted here — clients should call
 * {@code DELETE /cart/items/{itemId}} to remove a line, so the intent
 * is always explicit.
 */
public record UpdateCartItemRequest(

        @NotNull
        @Min(value = 1, message = "Quantity must be at least 1. Use DELETE to remove a line.")
        @Max(value = 20, message = "Quantity per line is capped at 20.")
        Integer quantity
) {
}
