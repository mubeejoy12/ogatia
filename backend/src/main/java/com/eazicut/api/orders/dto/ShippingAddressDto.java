package com.eazicut.api.orders.dto;

/**
 * Wire shape for a shipping address on an {@link OrderResponse}.
 *
 * <p>Optional fields ({@code addressLine2}, {@code region},
 * {@code postalCode}) are nullable on the wire — the DB column is
 * nullable and empty strings from the form are normalised to null
 * at persist time.
 */
public record ShippingAddressDto(
        String fullName,
        String email,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String region,
        String postalCode,
        String country
) {
}
