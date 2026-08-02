package com.eazicut.api.orders.service;

import java.security.SecureRandom;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.eazicut.api.orders.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

/**
 * Generates human-facing order references in the format
 * {@code EAZI-<epoch-ms>-<hex4>}.
 *
 * <p>Matches the pre-B006 localStorage prototype's convention
 * (see former {@code src/features/orders/types.ts}) so support staff
 * don't have to retrain. The epoch prefix gives references a natural
 * chronological order in a log dump; the four random hex characters
 * add ~65k of headroom per millisecond against collisions.
 *
 * <p><strong>Uniqueness.</strong> The DB has
 * {@code ux_order_reference} — a duplicate insert would throw a
 * {@code DataIntegrityViolationException} caught by
 * {@code GlobalExceptionHandler} as 409. This generator does a
 * pre-write probe ({@code orderRepository.existsByReference}) and
 * regenerates on collision, up to {@link #MAX_ATTEMPTS} times, so
 * the happy path never surfaces a 409 for reasons the customer
 * can't act on. A bounded retry (rather than an infinite loop)
 * means a persistent clash — e.g. clock jitter combined with a
 * very unlucky RNG — surfaces loudly as
 * {@link IllegalStateException} instead of hanging the request.
 */
@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    static final int MAX_ATTEMPTS = 5;
    private static final String PREFIX = "EAZI";
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private final OrderRepository orderRepository;
    private final SecureRandom random = new SecureRandom();

    public String generateUnique() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = build();
            if (!orderRepository.existsByReference(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Failed to generate a unique order reference after "
                        + MAX_ATTEMPTS + " attempts.");
    }

    private String build() {
        long epochMs = Instant.now().toEpochMilli();
        char[] suffix = new char[4];
        for (int i = 0; i < suffix.length; i++) {
            suffix[i] = HEX[random.nextInt(HEX.length)];
        }
        return PREFIX + "-" + epochMs + "-" + new String(suffix);
    }
}
