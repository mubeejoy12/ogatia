package com.eazicut.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Baseline security configuration for a stateless JSON API.
 *
 * <ul>
 *   <li>CSRF disabled — safe for a token-based JSON API; if a session
 *       or cookie strategy is later introduced, this needs re-thinking.</li>
 *   <li>Session policy: stateless — no server session created per request.</li>
 *   <li>All requests currently permitted — this is a scaffold only.
 *       Endpoint-level authorization rules are added as features arrive.</li>
 * </ul>
 *
 * <p>Actuator's default {@code /actuator/health} remains publicly readable
 * for uptime probes; other actuator endpoints are gated by the actuator
 * configuration in {@code application.yml}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
