package com.eazicut.api.orders.dto;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code POST /api/v1/orders}.
 *
 * <p><strong>Never trust prices from the frontend.</strong> The
 * {@link #expectedTotal} field is the customer's understanding of the
 * charge — it is the value the server compares against its own
 * live-price recompute, and if the two disagree the create is
 * refused with {@code price_mismatch} (409). Per-line prices are
 * intentionally NOT accepted — the server sources them from the cart
 * (which has snapshots) and re-reads live product prices for the
 * authoritative amount.
 *
 * <p><strong>Idempotency-Key is not in the body</strong> — it lives
 * in the {@code Idempotency-Key} HTTP header (Stage 3 endpoint). This
 * separates transport concerns from the domain payload and matches
 * the wider industry convention.
 */
public record CreateOrderRequest(

        /**
         * Id of the delivery method the customer chose. Must match
         * one of the ids from {@code DeliveryMethodCatalog} — a stale
         * or fabricated id → 400 {@code unknown_delivery_method}.
         */
        @NotBlank
        @Size(max = 64)
        String deliveryMethodId,

        @NotNull
        @Valid
        CreateShippingAddressRequest shippingAddress,

        /**
         * Customer-visible total the frontend showed on the review
         * screen. The server refuses the create with 409
         * {@code price_mismatch} if the current live-price recompute
         * disagrees. Prevents the customer from being silently
         * charged more (or less) than they thought.
         */
        @NotNull
        @DecimalMin(value = "0.00", message = "expectedTotal cannot be negative.")
        @Digits(integer = 15, fraction = 4)
        BigDecimal expectedTotal
) {
}
