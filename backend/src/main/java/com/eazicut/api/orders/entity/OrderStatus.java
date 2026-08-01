package com.eazicut.api.orders.entity;

/**
 * Lifecycle status for an {@link Order}.
 *
 * <p>Persisted as {@code STRING} + {@code @JdbcTypeCode(SqlTypes.VARCHAR)}
 * (same pattern {@code ProductStatus} uses) so storage stays
 * human-readable and portable across H2 and PostgreSQL.
 *
 * <p><strong>B006 scope.</strong> Only {@link #PENDING_PAYMENT} is
 * written by the create-order flow (Stage 2); admin transitions to
 * {@link #CANCELLED} land in Stage 4. Every other value is a reserved
 * seat:
 *
 * <ul>
 *   <li>{@link #PAID} — the payment webhook (B007) marks this on
 *       successful capture.</li>
 *   <li>{@link #FULFILLING} — atelier accepted the piece; the
 *       commission is on the cutting table.</li>
 *   <li>{@link #SHIPPED} — dispatched to the customer.</li>
 *   <li>{@link #DELIVERED} — customer signed for it.</li>
 *   <li>{@link #REFUNDED} — atelier issued a refund via the payment
 *       provider.</li>
 * </ul>
 *
 * <p>Shipping the full enum now means B007 and admin-fulfilment
 * tickets don't need a schema migration.
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    FULFILLING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
