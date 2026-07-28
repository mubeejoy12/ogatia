package com.eazicut.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login payload. Public — anyone can POST this.
 *
 * <p>Validation is deliberately minimal: {@code @NotBlank} on both
 * fields, plus a length cap so an attacker cannot make the server
 * BCrypt-hash a 10 MB "password" per attempt. Everything else is the
 * service's job:
 *
 * <ul>
 *   <li>{@code AuthService.login} normalises the email (trim + lowercase)
 *       before lookup.</li>
 *   <li>Password shape/blocklist rules from {@code @ValidPassword} do
 *       NOT apply here — a legacy or blocklisted password should still
 *       be able to sign in (and the response is the same "invalid
 *       credentials" whether the account exists or not).</li>
 * </ul>
 */
public record LoginRequest(

        @NotBlank
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(max = 128)
        String password
) {
}
