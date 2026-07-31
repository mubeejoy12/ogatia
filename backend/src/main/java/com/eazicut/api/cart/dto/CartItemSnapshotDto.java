package com.eazicut.api.cart.dto;

import java.math.BigDecimal;

/**
 * Point-of-add snapshot embedded in {@link CartItemResponse}.
 *
 * <p>Historical / display only. The {@code price} here is the value
 * captured when the line was added; the sibling {@code currentPrice}
 * on {@link CartItemResponse} is fetched live from the product table.
 * See {@code CartItem} Javadoc for the "never silently becomes charged
 * price" invariant.
 */
public record CartItemSnapshotDto(
        String name,
        String slug,
        BigDecimal price,
        String currency,
        String imageUrl
) {
}
