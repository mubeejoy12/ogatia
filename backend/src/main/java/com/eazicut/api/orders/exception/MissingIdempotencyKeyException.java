package com.eazicut.api.orders.exception;

/**
 * Thrown when {@code POST /orders} arrives without an
 * {@code Idempotency-Key} header — a required contract per B006
 * decision D4.
 *
 * <p>Handled by {@code GlobalExceptionHandler} as HTTP 400
 * ({@code missing_idempotency_key}). A missing key would allow
 * duplicate orders on retry / double-click; refusing loud is the
 * safe answer.
 */
public class MissingIdempotencyKeyException extends RuntimeException {

    public MissingIdempotencyKeyException() {
        super("The Idempotency-Key header is required for order creation.");
    }
}
