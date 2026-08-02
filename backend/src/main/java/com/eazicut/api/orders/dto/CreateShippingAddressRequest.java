package com.eazicut.api.orders.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Nested request record for the shipping address on a
 * {@link CreateOrderRequest}.
 *
 * <p>Field constraints match the DB column widths (V7 migration) so a
 * validation failure fires at the request boundary, not on flush.
 * Optional fields ({@code addressLine2}, {@code region},
 * {@code postalCode}) are unconstrained beyond size — an empty string
 * is normalised to null during persist so the DB column NULLs match
 * the intent.
 */
public record CreateShippingAddressRequest(

        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 50) String phone,

        @NotBlank @Size(max = 300) String addressLine1,
        @Size(max = 300) String addressLine2,

        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String region,
        @Size(max = 30) String postalCode,

        @NotBlank @Size(max = 100) String country
) {
}
