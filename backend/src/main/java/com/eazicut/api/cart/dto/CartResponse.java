package com.eazicut.api.cart.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full wire shape for a cart.
 *
 * <p>Server-authoritative: every mutating endpoint (Stage 2 onwards)
 * returns the full updated {@code CartResponse} so the frontend
 * replaces its state atomically without local reconciliation.
 *
 * @param id         cart id (opaque to the client; never used in URLs)
 * @param currency   cart-level currency (single-currency at launch)
 * @param items      lines in add order
 * @param subtotal   sum of {@code snapshot.price × quantity} across
 *                   available lines. Historical/display total —
 *                   authoritative charged amount is decided at Order
 *                   time. See CartItem Javadoc.
 * @param itemCount  sum of quantities across all lines
 * @param issues     read-time observations for the UI (Stage 3 populates)
 * @param updatedAt  most recent mutation time
 */
public record CartResponse(
        UUID id,
        String currency,
        List<CartItemResponse> items,
        BigDecimal subtotal,
        int itemCount,
        List<CartIssueResponse> issues,
        Instant updatedAt
) {
}
