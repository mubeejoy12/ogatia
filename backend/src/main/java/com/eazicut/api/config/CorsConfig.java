package com.eazicut.api.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cross-origin configuration for the Next.js frontend.
 *
 * <p>Allowed origins are supplied via the <code>ALLOWED_ORIGINS</code>
 * environment variable (comma-separated). Defaults to
 * <code>http://localhost:3000</code> for local development.
 *
 * <p>Explicitly disallows wildcard origins in production — every deploy
 * must name the frontend host it trusts.
 *
 * <p>Publishes a {@link CorsConfigurationSource} bean rather than a
 * standalone {@code CorsFilter}. Spring Security consumes this bean
 * when {@code .cors(Customizer.withDefaults())} is enabled on the
 * filter chain (see {@code SecurityConfig}); that way CORS headers
 * are added at exactly the right place in the servlet pipeline
 * (before the authorization decision) so both preflight AND
 * authenticated cross-origin responses get the right headers.
 */
@Configuration
public class CorsConfig {

    @Value("${eazicut.allowed-origins:http://localhost:3000}")
    private String allowedOriginsCsv;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Location", "X-Request-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
