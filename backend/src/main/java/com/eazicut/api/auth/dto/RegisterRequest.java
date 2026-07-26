package com.eazicut.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.eazicut.api.auth.validation.ValidPassword;

/**
 * Registration payload. Public — anyone can POST this.
 *
 * <p>Per D3 (minimal launch scope): email + password + optional display
 * name. Email verification, password reset, MFA, and social login are
 * deferred to later tickets.
 *
 * <p><strong>Email.</strong> {@code @Email} for shape validation. The
 * service normalises (trim + lowercase) before probing uniqueness and
 * storing — see {@code AuthService.register}.
 *
 * <p><strong>Password.</strong> {@link ValidPassword} enforces the 8–128
 * length rule and the common-password blocklist. The raw password never
 * leaves the service — {@code PasswordEncoder.encode} runs at the
 * boundary and only the hash lands in the DB.
 *
 * <p><strong>Display name.</strong> Optional; capped at 120 chars to
 * match the DB column. When absent, {@code /auth/me} responses (Stage 6)
 * fall back to the email local-part for greetings.
 */
public record RegisterRequest(

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @ValidPassword
        String password,

        @Size(max = 120)
        String displayName
) {
}
