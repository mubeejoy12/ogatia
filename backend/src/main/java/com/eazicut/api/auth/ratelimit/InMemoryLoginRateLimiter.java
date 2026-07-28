package com.eazicut.api.auth.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import com.eazicut.api.auth.exception.TooManyLoginAttemptsException;

/**
 * In-memory implementation of {@link LoginRateLimiter}.
 *
 * <p><strong>Model.</strong> A rolling window per key: on the first
 * failure a bucket is opened with {@code count = 1} and
 * {@code windowStart = now}. Subsequent failures within the window
 * bump {@code count}. Once {@code count > MAX_ATTEMPTS - 1} = 4, the
 * fifth call fails the {@link #assertAllowed} check; the block
 * persists until {@code windowStart + WINDOW} passes. On a
 * post-window failure the bucket resets.
 *
 * <p><strong>Bounds and hygiene.</strong> The map is unbounded in the
 * worst case (unique attackers can grow it). Two mitigations, both
 * cheap: (1) the bucket is discarded lazily on the next
 * {@link #assertAllowed} for that key once the window has passed;
 * (2) an accidental hot-key never blocks longer than {@code WINDOW}
 * so the working set stays proportional to concurrent attempts, not
 * total history. Good enough for launch (single instance, low volume);
 * the interface is Redis-ready for later.
 *
 * <p>Uses a {@link Clock} so tests can pin time deterministically
 * without {@code Thread.sleep}.
 */
@Component
public class InMemoryLoginRateLimiter implements LoginRateLimiter {

    static final int MAX_ATTEMPTS = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Clock clock;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public InMemoryLoginRateLimiter() {
        this(Clock.systemUTC());
    }

    // Package-private ctor for tests
    InMemoryLoginRateLimiter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void assertAllowed(String ip, String email) {
        assertKey(ipKey(ip));
        assertKey(emailKey(email));
    }

    @Override
    public void recordFailure(String ip, String email) {
        recordFailure(ipKey(ip));
        recordFailure(emailKey(email));
    }

    @Override
    public void recordSuccess(String ip, String email) {
        // Reset the email counter only — see interface Javadoc for why the
        // IP counter is left alone.
        buckets.remove(emailKey(email));
    }

    private void assertKey(String key) {
        Bucket b = buckets.get(key);
        if (b == null) return;
        Instant now = clock.instant();
        if (now.isAfter(b.windowStart.plus(WINDOW))) {
            // Window has passed — bucket is stale; discard it lazily so a
            // returning honest client isn't punished for old failures.
            buckets.remove(key, b);
            return;
        }
        if (b.count >= MAX_ATTEMPTS) {
            Duration remaining = Duration.between(now, b.windowStart.plus(WINDOW));
            throw new TooManyLoginAttemptsException(remaining);
        }
    }

    private void recordFailure(String key) {
        Instant now = clock.instant();
        buckets.compute(key, (k, existing) -> {
            if (existing == null || now.isAfter(existing.windowStart.plus(WINDOW))) {
                return new Bucket(now, 1);
            }
            return new Bucket(existing.windowStart, existing.count + 1);
        });
    }

    private static String ipKey(String ip)       { return "ip:" + (ip == null ? "unknown" : ip); }
    private static String emailKey(String email) { return "email:" + (email == null ? "" : email.trim().toLowerCase()); }

    /**
     * Rolling-window counter. Immutable so {@link #recordFailure}'s
     * compute() lambda can replace the value atomically without
     * intra-bucket locking.
     */
    private record Bucket(Instant windowStart, int count) {}
}
