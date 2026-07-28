package com.eazicut.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;

import com.eazicut.api.auth.jwt.JwtAuthenticationFilter;
import com.eazicut.api.auth.jwt.JwtProperties;

/**
 * Security configuration for the API.
 *
 * <p><strong>Stage 3 posture.</strong>
 * <ul>
 *   <li>{@link JwtAuthenticationFilter} runs before Spring's
 *       {@link UsernamePasswordAuthenticationFilter}. If a valid
 *       {@code Authorization: Bearer <jwt>} is present the request is
 *       already authenticated by the time the authorization rules run.</li>
 *   <li>{@code .anyRequest().authenticated()} — the default is now
 *       <em>secure</em>. Any newly-added endpoint is protected until it
 *       is either annotated with {@code @PreAuthorize} + explicitly
 *       allowlisted here (public) or reached with a valid credential.
 *       This is the single change that eliminates the pre-B004
 *       silent-drift risk.</li>
 *   <li>Explicit public allowlist below covers exactly the endpoints
 *       the audit designated public — nothing more.</li>
 *   <li>HTTP Basic was retired in Stage 4 now that {@code /auth/login}
 *       exists to mint a proper JWT. Bearer via
 *       {@link JwtAuthenticationFilter} is the sole wire credential
 *       mechanism.</li>
 *   <li>Session policy stays {@code STATELESS}; CSRF stays disabled.
 *       See the class-level doc on the previous B004 stage — bearer JWT
 *       + no ambient session + {@code SameSite=Lax} refresh cookie
 *       (Stage 5) mean CSRF is inapplicable.</li>
 * </ul>
 *
 * <p>The {@code EndpointAuthorizationIT} integration test walks every
 * mapped controller endpoint and asserts the expected auth level, so
 * the allowlist below can't silently drift out of sync with the app.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // --- Public: system probes ---
                        .requestMatchers("/health", "/actuator/health", "/actuator/info").permitAll()
                        // --- Public: read-only catalogue ---
                        .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/collections/**").permitAll()
                        // --- Public: auth entry points (Stages 2, 4, 5) ---
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
                        // --- Everything else requires an authenticated principal ---
                        .anyRequest().authenticated()
                )
                // HTTP Basic was removed in Stage 4 now that /auth/login
                // exists to mint a proper JWT. Bearer via JwtAuthenticationFilter
                // is the only wire credential mechanism.
                //
                // Without HTTP Basic's own AuthenticationEntryPoint, Spring's
                // default would answer missing-credential requests with 403.
                // For a bearer-only API 401 is the semantically correct answer
                // ("authenticate please"); 403 is reserved for "authenticated
                // but not authorised".
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
