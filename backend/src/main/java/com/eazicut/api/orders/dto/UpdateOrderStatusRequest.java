package com.eazicut.api.orders.dto;

import jakarta.validation.constraints.NotNull;

import com.eazicut.api.orders.entity.OrderStatus;

/**
 * Payload for {@code PATCH /api/v1/admin/orders/{id}/status}.
 *
 * <p>Body is deliberately narrow — only the new status. Reason /
 * note fields for refunds and cancellations are a natural
 * follow-up ticket; adding them now would design for hypothetical
 * future requirements before B006's launch scope is landed.
 *
 * <p>Enum shape means Spring rejects unknown strings at binding
 * time with a 400 {@code invalid_parameter} via the global
 * handler's {@code MethodArgumentTypeMismatchException} — no need
 * for an extra bean-validation constraint.
 */
public record UpdateOrderStatusRequest(

        @NotNull
        OrderStatus status
) {
}
