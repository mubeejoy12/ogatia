package com.eazicut.api.orders.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.eazicut.api.orders.entity.OrderStatus;

/**
 * Full wire shape for an {@link com.eazicut.api.orders.entity.Order Order}.
 *
 * <p>Both admin and customer endpoints return this shape. Ownership
 * checks live at the repository / service layer — a customer can
 * only fetch their own orders. No field is admin-only.
 *
 * <p>{@code idempotencyKey} is deliberately NOT surfaced. It's an
 * implementation detail of the create-order flow (Stage 2); leaking
 * it doesn't reveal anything sensitive but adds noise the frontend
 * doesn't need.
 */
public record OrderResponse(
        UUID id,
        String reference,
        OrderStatus status,
        String currency,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal total,
        String deliveryMethodId,
        String deliveryMethodName,
        ShippingAddressDto shippingAddress,
        List<OrderItemResponse> items,
        Instant placedAt,
        Instant updatedAt
) {
}
