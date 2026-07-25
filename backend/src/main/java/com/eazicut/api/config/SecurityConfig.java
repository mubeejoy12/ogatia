package com.eazicut.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the API.
 *
 * <p><strong>Where B004 is right now.</strong> Stage 1 replaces the
 * Spring Boot in-memory user store with a DB-backed
 * {@code JpaUserDetailsService} and adds a proper {@link PasswordEncoder}
 * bean. HTTP Basic is still the wire mechanism until Stage 3 introduces
 * JWT; {@code .anyRequest().permitAll()} still stands until Stage 3
 * flips the default to {@code authenticated} with an explicit public
 * allowlist.
 *
 * <p>Session policy stays {@code STATELESS} — the target architecture is
 * JWT with a refresh cookie; nothing here needs to persist a session.
 *
 * <p>CSRF stays disabled. The API is stateless, will move to bearer JWT
 * (no ambient credentials), and the refresh-token cookie (Stage 5) will
 * be {@code SameSite=Lax}. Documented here so the reason survives the
 * next reader — do NOT re-enable CSRF without also introducing a
 * cookie-carried session for state-changing requests.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * BCrypt with the library default cost (10 rounds ~ 100 ms on a
     * dev laptop). Centralising the encoder here means Stage 2's
     * registration flow and any future re-hash migration go through
     * one place — bump the cost, rehash-on-login, or swap to Argon2
     * with a single edit.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expose the framework-built {@link AuthenticationManager} so the
     * later {@code AuthService.login()} (Stage 4) can drive the same
     * authentication pipeline Spring uses for HTTP Basic, without
     * hand-rolling password comparison.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // HTTP Basic remains the wire credential in Stage 1 so the
                // admin curls that B002/B003 rely on keep working. Stage 3
                // replaces this with the OAuth2 resource-server JWT filter
                // and flips the default rule to `authenticated`.
                .httpBasic(basic -> {})
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
