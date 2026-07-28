package com.eazicut.api.auth.exception;

import java.time.Duration;

/**
 * Raised by the {@code LoginRateLimiter} when either the calling IP or
 * the target email has exceeded its permitted failure count inside the
 * rolling window.
 *
 * <p>Handled by {@code GlobalExceptionHandler} as HTTP 429 Too Many
 * Requests. Carries the {@link #retryAfter} interval so the handler
 * can populate a {@code Retry-After} response header — clients
 * (browsers, our own frontend) can then back off gracefully.
 */
public class TooManyLoginAttemptsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyLoginAttemptsException(Duration retryAfter) {
        super("Too many login attempts. Try again in %d seconds."
                .formatted(Math.max(1, retryAfter.toSeconds())));
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
