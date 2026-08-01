package com.eazicut.api.orders.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Immutable-by-convention shipping address embedded on {@link Order}.
 *
 * <p>Deliberately flat columns on {@code orders} (no separate table)
 * — no address-book at launch, and orders capture shipping details
 * at a point in time. A later address-book ticket seeds itself FROM
 * these rows, not the other way round.
 *
 * <p>Column names align with the V7 migration exactly.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ShippingAddress {

    @Column(name = "shipping_full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "shipping_email", nullable = false, length = 255)
    private String email;

    @Column(name = "shipping_phone", nullable = false, length = 50)
    private String phone;

    @Column(name = "shipping_address_line1", nullable = false, length = 300)
    private String addressLine1;

    @Column(name = "shipping_address_line2", length = 300)
    private String addressLine2;

    @Column(name = "shipping_city", nullable = false, length = 100)
    private String city;

    @Column(name = "shipping_region", length = 100)
    private String region;

    @Column(name = "shipping_postal_code", length = 30)
    private String postalCode;

    @Column(name = "shipping_country", nullable = false, length = 100)
    private String country;
}
