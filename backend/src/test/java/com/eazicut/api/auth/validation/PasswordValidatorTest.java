package com.eazicut.api.auth.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Direct unit tests for the {@link PasswordValidator} rule.
 *
 * <p>Tests the rule engine directly rather than driving it through Bean
 * Validation — the framework wiring is exercised end-to-end via the
 * live curl gauntlet at the end of this stage.
 */
class PasswordValidatorTest {

    private final PasswordValidator validator = new PasswordValidator();

    @Test @DisplayName("accepts a straightforward 12-char password")
    void acceptsGoodPassword() {
        assertThat(validator.isValid("correct horse battery", null)).isTrue();
    }

    @Test @DisplayName("accepts exactly the minimum length (8)")
    void acceptsMinLength() {
        assertThat(validator.isValid("aB3!zY9x", null)).isTrue();
    }

    @Test @DisplayName("accepts exactly the maximum length (128)")
    void acceptsMaxLength() {
        assertThat(validator.isValid("x".repeat(128), null)).isTrue();
    }

    @Test @DisplayName("rejects 7-char password (below MIN_LENGTH)")
    void rejectsBelowMin() {
        assertThat(validator.isValid("short7!", null)).isFalse();
    }

    @Test @DisplayName("rejects 129-char password (above MAX_LENGTH)")
    void rejectsAboveMax() {
        assertThat(validator.isValid("x".repeat(129), null)).isFalse();
    }

    @Test @DisplayName("rejects blocklist entry regardless of case")
    void rejectsBlocklist() {
        assertThat(validator.isValid("password", null)).isFalse();
        assertThat(validator.isValid("PASSWORD", null)).isFalse();
        assertThat(validator.isValid("Password", null)).isFalse();
        assertThat(validator.isValid("EAZICUT", null)).isFalse();
        assertThat(validator.isValid("qwerty", null)).isFalse();
        assertThat(validator.isValid("12345678", null)).isFalse();
    }

    @Test @DisplayName("null and blank pass through — @NotBlank handles emptiness separately")
    void nullAndBlankAreNeutral() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test @DisplayName("does NOT enforce complexity classes — a-only string of correct length passes")
    void noForcedComplexity() {
        assertThat(validator.isValid("aaaaaaaa", null)).isTrue();
    }
}
