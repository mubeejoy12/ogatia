package com.eazicut.api.auth.ratelimit;

import com.eazicut.api.auth.exception.TooManyLoginAttemptsException;

/**
 * Two-dimensional rate limit for {@code POST /auth/login}.
 *
 * <p>Every login attempt is counted independently against the calling
 * IP and against the target email. Either counter reaching the
 * threshold blocks the request for the remaining window — an attacker
 * cannot side-step the limit by rotating emails on one IP, nor by
 * spraying one email across many IPs.
 *
 * <p>Deliberately interface-first per D7 — the in-memory implementation
 * is fine for a single-instance launch, but replacing it with a
 * Redis/Bucket4j-backed shared counter (once we scale beyond one pod)
 * is a bean-swap and nothing more. The controller and service depend
 * only on this contract.
 */
public interface LoginRateLimiter {

    /**
     * Verify both counters are under threshold. Throws
     * {@link TooManyLoginAttemptsException} carrying the seconds left
     * in the current window if either dimension is exhausted.
     */
    void assertAllowed(String ip, String email);

    /**
     * Record a failed login. Increments both counters; the next
     * {@link #assertAllowed} call sees the update.
     */
    void recordFailure(String ip, String email);

    /**
     * Record a successful login. Resets the email counter (the account
     * clearly belongs to whoever just signed in). Leaves the IP counter
     * untouched — a shared IP might still be hosting other malicious
     * attempts we don't want to acquit prematurely.
     */
    void recordSuccess(String ip, String email);
}
