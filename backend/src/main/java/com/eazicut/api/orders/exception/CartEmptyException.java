package com.eazicut.api.orders.exception;

/**
 * Thrown by {@code OrderService.createFromCart} when the caller's
 * cart holds zero lines at order-creation time.
 *
 * <p>Handled by {@code GlobalExceptionHandler} as HTTP 400 with error
 * slug {@code cart_empty}. Distinct from the 4xx {@code conflict}
 * codes because the request itself is malformed (there's nothing to
 * order), not because of a semantic drift between request and world.
 */
public class CartEmptyException extends RuntimeException {

    public CartEmptyException() {
        super("Your cart is empty — add a piece before placing an order.");
    }
}
