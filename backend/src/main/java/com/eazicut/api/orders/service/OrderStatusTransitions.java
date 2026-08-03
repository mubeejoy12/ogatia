package com.eazicut.api.orders.service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.eazicut.api.orders.entity.OrderStatus;

/**
 * Pure lookup table for allowed {@link OrderStatus} transitions.
 *
 * <p>No Spring, no state — a static map consulted by
 * {@code OrderService.adminUpdateStatus}. Isolating the rules here
 * keeps them testable in one place and lets future stages (B007
 * payment webhook, atelier fulfilment UI) reuse the same guard.
 *
 * <p><strong>Rule set (launch).</strong>
 * <ul>
 *   <li>{@code PENDING_PAYMENT} → {@code PAID} (B007 webhook — admin
 *       may also do it manually for cash/bank-transfer orders) or
 *       {@code CANCELLED} (customer changed their mind / atelier
 *       cancels stale).</li>
 *   <li>{@code PAID} → {@code FULFILLING} (atelier accepts the
 *       commission) or {@code REFUNDED}.</li>
 *   <li>{@code FULFILLING} → {@code SHIPPED} or {@code REFUNDED}.</li>
 *   <li>{@code SHIPPED} → {@code DELIVERED} or {@code REFUNDED}.</li>
 *   <li>{@code DELIVERED} → {@code REFUNDED} (post-delivery
 *       return).</li>
 *   <li>{@code CANCELLED} and {@code REFUNDED} are terminal — no
 *       transitions out.</li>
 * </ul>
 *
 * <p>Transitioning a status to itself is refused — clients that need
 * to "touch" an order without moving it should not pretend the
 * PATCH is a no-op.
 */
public final class OrderStatusTransitions {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED =
            new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(OrderStatus.PENDING_PAYMENT, EnumSet.of(
                OrderStatus.PAID,
                OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.PAID, EnumSet.of(
                OrderStatus.FULFILLING,
                OrderStatus.REFUNDED));
        ALLOWED.put(OrderStatus.FULFILLING, EnumSet.of(
                OrderStatus.SHIPPED,
                OrderStatus.REFUNDED));
        ALLOWED.put(OrderStatus.SHIPPED, EnumSet.of(
                OrderStatus.DELIVERED,
                OrderStatus.REFUNDED));
        ALLOWED.put(OrderStatus.DELIVERED, EnumSet.of(
                OrderStatus.REFUNDED));
        ALLOWED.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.REFUNDED,  EnumSet.noneOf(OrderStatus.class));
    }

    private OrderStatusTransitions() {}

    /** True when {@code from → to} is a permitted admin transition. */
    public static boolean isAllowed(OrderStatus from, OrderStatus to) {
        if (from == null || to == null || from == to) return false;
        return ALLOWED.getOrDefault(from, EnumSet.noneOf(OrderStatus.class))
                .contains(to);
    }

    /** Terminal statuses — no outward transitions permitted. */
    public static boolean isTerminal(OrderStatus status) {
        return status != null && ALLOWED.getOrDefault(status, EnumSet.noneOf(OrderStatus.class)).isEmpty();
    }
}
