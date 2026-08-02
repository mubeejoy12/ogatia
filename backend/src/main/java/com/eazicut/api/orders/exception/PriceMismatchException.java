package com.eazicut.api.orders.exception;

import java.math.BigDecimal;

import com.eazicut.api.common.exception.ConflictException;

/**
 * Thrown when the customer's {@code expectedTotal} on
 * {@link com.eazicut.api.orders.dto.CreateOrderRequest} disagrees
 * with the server's live-price recompute.
 *
 * <p>Encodes the B005 pricing invariant at the enforcement point: a
 * stale price the customer saw on the checkout page must NEVER
 * silently become the charged amount. If the recompute differs, the
 * create is refused with 409 {@code price_mismatch} and the current
 * server total surfaces in the message so the client can render
 * "prices updated — please review".
 *
 * <p>Handled by {@code GlobalExceptionHandler} as HTTP 409.
 */
public class PriceMismatchException extends ConflictException {

    private final BigDecimal currentTotal;

    public PriceMismatchException(BigDecimal expected, BigDecimal current) {
        super("Total changed while you were checking out: expected %s, current %s. Please review before placing the order."
                .formatted(expected.toPlainString(), current.toPlainString()));
        this.currentTotal = current;
    }

    public BigDecimal currentTotal() {
        return currentTotal;
    }
}
