package com.eazicut.api.auth.jwt;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.eazicut.api.auth.jwt.JwtService.ParsedAccessToken;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

/**
 * Servlet filter that turns a valid {@code Authorization: Bearer <jwt>}
 * header into a populated {@link SecurityContextHolder}.
 *
 * <p><strong>Behaviour by header state:</strong>
 * <ul>
 *   <li>No {@code Authorization} header — pass through unchanged. The
 *       downstream filter chain treats the request as anonymous; the
 *       filter chain's public allowlist or {@code @PreAuthorize} decides
 *       whether it's allowed.</li>
 *   <li>Header present but not {@code Bearer } — pass through. Might be
 *       Basic (kept transitionally in Stage 3), which Spring's own
 *       {@code BasicAuthenticationFilter} handles later in the chain.</li>
 *   <li>Bearer token present but invalid (expired, bad signature,
 *       tampered) — <em>clear</em> the context and pass through. The
 *       chain's authorization rules then reject with 401. We do not
 *       write a 401 here so the {@code AuthenticationEntryPoint} can
 *       shape the response body through the global handler.</li>
 *   <li>Bearer token valid — build a
 *       {@link UsernamePasswordAuthenticationToken} carrying the user's
 *       email as principal and the {@code ROLE_*} authority derived
 *       from the token's {@code role} claim. No DB lookup.</li>
 * </ul>
 *
 * <p>{@link OncePerRequestFilter} guarantees exactly-once execution
 * within async or forward dispatches.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            ParsedAccessToken parsed = jwtService.parse(token);

            var auth = new UsernamePasswordAuthenticationToken(
                    parsed.email(),
                    null,
                    List.of(new SimpleGrantedAuthority(parsed.role().authority()))
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException | IllegalArgumentException ex) {
            // Invalid token — clear any prior context and let the
            // authorization rules produce 401 downstream.
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
