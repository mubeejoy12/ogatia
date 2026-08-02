package com.eazicut.api.orders.exception;

/**
 * Thrown when {@code CreateOrderRequest.deliveryMethodId} doesn't
 * match any entry in {@code DeliveryMethodCatalog}.
 *
 * <p>Handled by {@code GlobalExceptionHandler} as HTTP 400
 * ({@code unknown_delivery_method}). Semantically a bad request —
 * the id is either fabricated or refers to a method that's been
 * retired between the checkout page load and the submit.
 */
public class UnknownDeliveryMethodException extends RuntimeException {

    public UnknownDeliveryMethodException(String deliveryMethodId) {
        super("Unknown delivery method: '%s'.".formatted(deliveryMethodId));
    }
}
