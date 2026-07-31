package com.eazicut.api.cart.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Wire shape for one cart line.
 *
 * <p>Carries both the historical {@link #snapshot} (what the customer
 * saw when they added the piece) and the live {@link #currentPrice} +
 * {@link #available} (fetched from the product table right now). The
 * frontend surfaces the delta between the two — Stage 3 populates the
 * {@link CartResponse#issues()} list with actionable messages when
 * they diverge; this stage returns them cleanly so downstream stages
 * can add semantics without changing the contract.
 */
public record CartItemResponse(
        UUID id,
        UUID productId,
        String productSlug,
        String size,
        int quantity,
        CartItemSnapshotDto snapshot,
        BigDecimal currentPrice,
        boolean available,
        Instant addedAt,
        Instant updatedAt
) {
}
