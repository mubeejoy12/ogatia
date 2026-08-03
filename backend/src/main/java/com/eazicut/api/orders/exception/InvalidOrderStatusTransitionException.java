package com.eazicut.api.orders.exception;

import com.eazicut.api.common.exception.ConflictException;
import com.eazicut.api.orders.entity.OrderStatus;

/**
 * Thrown by {@code OrderService.adminUpdateStatus} when the caller
 * asks for an {@link OrderStatus} transition that isn't in the
 * allowed set (see {@code OrderStatusTransitions}).
 *
 * <p>Encoded as a 409 (semantic conflict between the request and the
 * order's current lifecycle state) with the specific slug
 * {@code invalid_status_transition} so the admin UI can render
 * "can't move a delivered order back to shipped" copy without
 * parsing the message.
 */
public class InvalidOrderStatusTransitionException extends ConflictException {

    public InvalidOrderStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Cannot transition order status from %s to %s.".formatted(from, to));
    }
}
