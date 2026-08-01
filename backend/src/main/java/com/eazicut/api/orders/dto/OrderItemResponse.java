package com.eazicut.api.orders.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Wire shape for one line of an order.
 *
 * <p>Represents an immutable charge record — see {@code OrderItem}
 * entity Javadoc. {@code unitPrice} is what was actually charged;
 * the product identity snapshots ({@code productName},
 * {@code productSlug}, {@code productImageUrl}) show what the
 * customer saw at purchase.
 */
public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productSlug,
        String productName,
        String productImageUrl,
        String size,
        int quantity,
        BigDecimal unitPrice,
        String currency,
        BigDecimal lineTotal
) {
}
