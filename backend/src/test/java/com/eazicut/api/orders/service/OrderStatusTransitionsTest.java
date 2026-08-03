package com.eazicut.api.orders.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.eazicut.api.orders.entity.OrderStatus;

/**
 * Pure-logic tests for the {@link OrderStatusTransitions} guard.
 * Every rule from the class Javadoc has a case here so the launch
 * lifecycle can't drift without a test flip.
 */
class OrderStatusTransitionsTest {

    @Test
    @DisplayName("PENDING_PAYMENT → PAID and CANCELLED are the only permitted outward moves")
    void fromPendingPayment() {
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PENDING_PAYMENT, OrderStatus.FULFILLING)).isFalse();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PENDING_PAYMENT, OrderStatus.SHIPPED)).isFalse();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PENDING_PAYMENT, OrderStatus.REFUNDED)).isFalse();
    }

    @Test
    @DisplayName("PAID → FULFILLING or REFUNDED")
    void fromPaid() {
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PAID, OrderStatus.FULFILLING)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PAID, OrderStatus.REFUNDED)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PAID, OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PAID, OrderStatus.SHIPPED)).isFalse();
    }

    @Test
    @DisplayName("FULFILLING → SHIPPED or REFUNDED")
    void fromFulfilling() {
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.FULFILLING, OrderStatus.SHIPPED)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.FULFILLING, OrderStatus.REFUNDED)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.FULFILLING, OrderStatus.DELIVERED)).isFalse();
    }

    @Test
    @DisplayName("SHIPPED → DELIVERED or REFUNDED")
    void fromShipped() {
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.SHIPPED, OrderStatus.DELIVERED)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.SHIPPED, OrderStatus.REFUNDED)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.SHIPPED, OrderStatus.PAID)).isFalse();
    }

    @Test
    @DisplayName("DELIVERED → REFUNDED only")
    void fromDelivered() {
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.DELIVERED, OrderStatus.REFUNDED)).isTrue();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.DELIVERED, OrderStatus.SHIPPED)).isFalse();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.DELIVERED, OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    @DisplayName("CANCELLED and REFUNDED are terminal — no outward transitions")
    void terminals() {
        assertThat(OrderStatusTransitions.isTerminal(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatusTransitions.isTerminal(OrderStatus.REFUNDED)).isTrue();
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(OrderStatusTransitions.isAllowed(OrderStatus.CANCELLED, target))
                    .as("no transition permitted from CANCELLED to %s", target).isFalse();
            assertThat(OrderStatusTransitions.isAllowed(OrderStatus.REFUNDED, target))
                    .as("no transition permitted from REFUNDED to %s", target).isFalse();
        }
    }

    @Test
    @DisplayName("Non-terminals are correctly classified")
    void nonTerminals() {
        assertThat(OrderStatusTransitions.isTerminal(OrderStatus.PENDING_PAYMENT)).isFalse();
        assertThat(OrderStatusTransitions.isTerminal(OrderStatus.PAID)).isFalse();
        assertThat(OrderStatusTransitions.isTerminal(OrderStatus.FULFILLING)).isFalse();
        assertThat(OrderStatusTransitions.isTerminal(OrderStatus.SHIPPED)).isFalse();
        assertThat(OrderStatusTransitions.isTerminal(OrderStatus.DELIVERED)).isFalse();
    }

    @Test
    @DisplayName("Same-status is NEVER a valid transition (PATCH-to-self is a client bug)")
    void sameStatusRefused() {
        for (OrderStatus s : OrderStatus.values()) {
            assertThat(OrderStatusTransitions.isAllowed(s, s))
                    .as("self-transition %s → %s must be false", s, s).isFalse();
        }
    }

    @Test
    @DisplayName("null inputs are safely refused")
    void nullsRefused() {
        assertThat(OrderStatusTransitions.isAllowed(null, OrderStatus.PAID)).isFalse();
        assertThat(OrderStatusTransitions.isAllowed(OrderStatus.PAID, null)).isFalse();
        assertThat(OrderStatusTransitions.isAllowed(null, null)).isFalse();
        assertThat(OrderStatusTransitions.isTerminal(null)).isFalse();
    }
}
