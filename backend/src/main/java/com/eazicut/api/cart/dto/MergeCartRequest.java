package com.eazicut.api.cart.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code POST /api/v1/cart/merge} — one-shot merge of a
 * guest (localStorage) cart into the authenticated user's server-side
 * cart, fired by the frontend immediately after login.
 *
 * <p>Guest lines are identified by {@code productSlug} (not
 * {@code productId}) because the pre-B005 localStorage cart snapshot
 * never captured product IDs — only slugs, from
 * {@code src/features/cart/types.ts}.
 *
 * <p><strong>Never fails "hard"</strong> on individual guest lines.
 * Unknown slugs, unavailable products, retired sizes, and overflow of
 * the per-cart line cap are silently skipped and reported through the
 * {@code CartResponse.issues[]} list, so a merge always yields a
 * usable cart. This is deliberately more lenient than the direct
 * {@code POST /cart/items} path — a guest may have accumulated stale
 * items over many sessions, and a login shouldn't fail because one
 * piece went out of stock in the meantime.
 *
 * <p>The whole payload is capped at {@link #MAX_INCOMING_LINES} to
 * limit request size; anything beyond is rejected at validation with
 * a 400.
 */
public record MergeCartRequest(

        @NotNull
        @Size(max = MAX_INCOMING_LINES,
                message = "Merge payload is capped at 100 lines.")
        @Valid
        List<GuestLine> lines
) {

    /**
     * Wire cap. Distinct from the per-cart line cap (50) — this
     * bounds the request itself. A merge payload larger than this is a
     * validation error; a merge payload that fits but exceeds the cart
     * cap after processing is trimmed silently with a
     * {@code cart_too_large} issue.
     */
    public static final int MAX_INCOMING_LINES = 100;

    public record GuestLine(

            @NotBlank
            @Size(max = 220)
            String productSlug,

            @NotBlank
            @Size(max = 32)
            String size,

            @NotNull
            @Min(value = 1, message = "Quantity must be at least 1.")
            @Max(value = 20, message = "Quantity per line is capped at 20.")
            Integer quantity
    ) {
    }
}
