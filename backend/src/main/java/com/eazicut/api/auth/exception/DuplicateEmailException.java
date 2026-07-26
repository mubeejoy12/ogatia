package com.eazicut.api.auth.exception;

import com.eazicut.api.common.exception.ConflictException;

/**
 * Thrown when a registration request carries an email that already has
 * a user row (case-insensitive comparison).
 *
 * <p>Handled by {@code GlobalExceptionHandler} as HTTP 409. Note this
 * does confirm to the caller that the email is registered — a mild
 * enumeration signal. D3 explicitly accepts that trade for launch;
 * mitigation (email verification with a "check your inbox" response
 * shape) arrives with the email-verification ticket.
 */
public class DuplicateEmailException extends ConflictException {

    public DuplicateEmailException(String email) {
        super("An account already exists for '%s'.".formatted(email));
    }
}
