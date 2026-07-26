package com.eazicut.api.auth.validation;

import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Concrete rules behind {@link ValidPassword}.
 *
 * <p>Split into two named checks so a caller (or an admin surface later)
 * can consult them directly without going through the framework:
 *
 * <ul>
 *   <li>{@link #MIN_LENGTH} / {@link #MAX_LENGTH} — length bounds.</li>
 *   <li>{@link #BLOCKLIST} — case-insensitive membership check.</li>
 * </ul>
 *
 * <p>The blocklist deliberately stays small (~20 entries) — the value is
 * not to enumerate every weak password (haveibeenpwned already does that
 * far better), it's to reject the specific footguns customers try first
 * when a form asks for a password: "password", "12345678", the brand name.
 * A future ticket can layer a haveibeenpwned range-query call on top.
 *
 * <p>A {@code null} or {@code blank} password fails silently here — the
 * caller is expected to also carry {@code @NotBlank} on the field so the
 * error message is precise. Doing the null check ourselves would double-
 * report on blank input.
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    /**
     * Low-effort common passwords + brand-specific ones customers reach
     * for by reflex. Comparison is case-insensitive. Kept short on
     * purpose — see class Javadoc.
     */
    public static final Set<String> BLOCKLIST = Set.of(
            "password",
            "password1",
            "password123",
            "12345678",
            "123456789",
            "1234567890",
            "qwerty",
            "qwerty123",
            "letmein",
            "welcome",
            "welcome1",
            "abc12345",
            "iloveyou",
            "admin",
            "administrator",
            "changeme",
            "eazicut",
            "eazicut123"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            // Let @NotBlank handle empty; don't double-report.
            return true;
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            return false;
        }
        return !BLOCKLIST.contains(value.toLowerCase());
    }
}
