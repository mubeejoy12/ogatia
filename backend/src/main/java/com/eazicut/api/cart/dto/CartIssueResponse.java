package com.eazicut.api.cart.dto;

import java.util.UUID;

/**
 * A read-time observation about a cart item — surfaced to the UI so
 * the customer can act (accept new price, remove unavailable line,
 * lower quantity) before checkout.
 *
 * <p>Stage 1 defines the shape and leaves {@link CartResponse#issues()}
 * empty; Stage 3 populates it. Codes are kept as strings on the wire
 * so the frontend can map them without needing an enum shipped in
 * lock-step.
 *
 * <p>Anticipated codes:
 * <ul>
 *   <li>{@code price_changed}       — snapshot price differs from current</li>
 *   <li>{@code out_of_stock}        — product is OUT_OF_STOCK or stock hit zero</li>
 *   <li>{@code product_removed}     — product has been soft-deleted</li>
 *   <li>{@code size_unavailable}    — the line's size is no longer offered</li>
 *   <li>{@code quantity_capped}     — quantity trimmed to the per-line cap</li>
 * </ul>
 *
 * @param itemId  cart line the observation refers to
 * @param code    machine-readable slug (see list above)
 * @param message human-readable copy already tailored for display
 */
public record CartIssueResponse(
        UUID itemId,
        String code,
        String message
) {
}
