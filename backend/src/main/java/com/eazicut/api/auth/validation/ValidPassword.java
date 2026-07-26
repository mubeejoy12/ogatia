package com.eazicut.api.auth.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Bean-Validation annotation for a user-supplied password.
 *
 * <p>Enforces the launch policy from D6: 8–128 characters, all printable
 * characters allowed (including spaces), no forced complexity classes,
 * plus a small blocklist of obvious passwords. The rule set is
 * NIST-ish — modern research says forced digits/symbols/mixed-case hurt
 * UX without meaningfully raising the entropy floor.
 *
 * <p>Wire it onto the {@code password} field of any registration or
 * password-change DTO. Violations surface through the existing
 * {@code MethodArgumentNotValidException} handler as HTTP 400 with the
 * uniform {@code validation_failed} shape.
 *
 * <p>See {@link PasswordValidator} for the concrete rule implementation.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
public @interface ValidPassword {
    String message() default "Password must be 8–128 characters and not on the common-password blocklist.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
