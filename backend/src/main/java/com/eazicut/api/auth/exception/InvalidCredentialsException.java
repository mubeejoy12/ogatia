package com.eazicut.api.auth.exception;

/**
 * Thrown by {@code AuthService.login} when the supplied credentials do
 * not match a live enabled user.
 *
 * <p>The message is <strong>deliberately generic</strong>: "Invalid
 * email or password." — the same shape whether the email is unknown,
 * the password is wrong, or the account is disabled. This closes the
 * enumeration side-channel (an attacker cannot tell whether an email
 * is registered by the login response).
 *
 * <p>Handled by {@code GlobalExceptionHandler} as HTTP 401 with the
 * uniform {@code ApiError} body.
 */
public class InvalidCredentialsException extends RuntimeException {

    private static final String GENERIC_MESSAGE = "Invalid email or password.";

    public InvalidCredentialsException() {
        super(GENERIC_MESSAGE);
    }
}
