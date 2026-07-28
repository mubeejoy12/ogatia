package com.eazicut.api.auth.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.eazicut.api.auth.exception.TooManyLoginAttemptsException;

/**
 * Unit tests for {@link InMemoryLoginRateLimiter}.
 *
 * <p>Uses a mutable {@link Clock} implementation so the rolling window
 * can be advanced deterministically — no {@code Thread.sleep}.
 */
class InMemoryLoginRateLimiterTest {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T12:00:00Z"));

    private final Clock clock = new Clock() {
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    };

    private final InMemoryLoginRateLimiter limiter = new InMemoryLoginRateLimiter(clock);

    @Test @DisplayName("assertAllowed — fresh IP + email passes without a bucket")
    void freshAllowed() {
        assertThatCode(() -> limiter.assertAllowed("1.2.3.4", "a@b.co")).doesNotThrowAnyException();
    }

    @Test @DisplayName("MAX_ATTEMPTS failures on same email block the next call")
    void blocksAfterMax() {
        for (int i = 0; i < InMemoryLoginRateLimiter.MAX_ATTEMPTS; i++) {
            limiter.recordFailure("1.2.3.4", "a@b.co");
        }
        assertThatThrownBy(() -> limiter.assertAllowed("1.2.3.4", "a@b.co"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
    }

    @Test @DisplayName("MAX_ATTEMPTS failures on same IP with rotating emails also block")
    void blocksSameIpRotatedEmails() {
        for (int i = 0; i < InMemoryLoginRateLimiter.MAX_ATTEMPTS; i++) {
            limiter.recordFailure("1.2.3.4", "user" + i + "@b.co");
        }
        assertThatThrownBy(() -> limiter.assertAllowed("1.2.3.4", "brand-new@b.co"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
    }

    @Test @DisplayName("recordSuccess resets the email counter (attempts on that email are forgiven)")
    void successClearsEmail() {
        for (int i = 0; i < InMemoryLoginRateLimiter.MAX_ATTEMPTS; i++) {
            limiter.recordFailure("9.9.9.9", "target@b.co");
        }
        // The IP is toast but the specific email should be cleared —
        // use a fresh IP to isolate the email dimension.
        limiter.recordSuccess("9.9.9.9", "target@b.co");
        assertThatCode(() -> limiter.assertAllowed("2.2.2.2", "target@b.co"))
                .doesNotThrowAnyException();
    }

    @Test @DisplayName("recordSuccess does NOT clear the IP counter (shared IP might host other bad actors)")
    void successDoesNotClearIp() {
        for (int i = 0; i < InMemoryLoginRateLimiter.MAX_ATTEMPTS; i++) {
            limiter.recordFailure("shared-ip", "user" + i + "@b.co");
        }
        limiter.recordSuccess("shared-ip", "user0@b.co");
        assertThatThrownBy(() -> limiter.assertAllowed("shared-ip", "another@b.co"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
    }

    @Test @DisplayName("bucket resets after WINDOW passes — honest client isn't punished forever")
    void bucketResetsAfterWindow() {
        for (int i = 0; i < InMemoryLoginRateLimiter.MAX_ATTEMPTS; i++) {
            limiter.recordFailure("3.3.3.3", "x@b.co");
        }
        // Advance past the window
        now.set(now.get().plus(InMemoryLoginRateLimiter.WINDOW).plus(Duration.ofSeconds(1)));
        assertThatCode(() -> limiter.assertAllowed("3.3.3.3", "x@b.co"))
                .doesNotThrowAnyException();
    }

    @Test @DisplayName("email dimension is case-insensitive (Signup@X vs signup@x hit the same bucket)")
    void emailIsCaseInsensitive() {
        for (int i = 0; i < InMemoryLoginRateLimiter.MAX_ATTEMPTS; i++) {
            limiter.recordFailure("4.4.4.4", "TARGET@Example.com");
        }
        // Same email, different casing — from a completely fresh IP
        // to isolate the email dimension.
        assertThatThrownBy(() -> limiter.assertAllowed("7.7.7.7", "target@example.com"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
    }

    @Test @DisplayName("TooManyLoginAttemptsException carries a positive retryAfter")
    void retryAfterCarried() {
        for (int i = 0; i < InMemoryLoginRateLimiter.MAX_ATTEMPTS; i++) {
            limiter.recordFailure("5.5.5.5", "y@b.co");
        }
        // Advance ~1 minute; still inside the 15-min window
        now.set(now.get().plus(Duration.ofMinutes(1)));
        try {
            limiter.assertAllowed("5.5.5.5", "y@b.co");
            throw new AssertionError("expected TooManyLoginAttemptsException");
        } catch (TooManyLoginAttemptsException ex) {
            assertThat(ex.retryAfter().toSeconds()).isBetween(1L, InMemoryLoginRateLimiter.WINDOW.toSeconds());
        }
    }
}
