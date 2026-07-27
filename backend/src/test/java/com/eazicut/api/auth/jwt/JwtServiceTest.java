package com.eazicut.api.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.eazicut.api.auth.jwt.JwtService.ParsedAccessToken;
import com.eazicut.api.users.entity.Role;
import com.eazicut.api.users.entity.User;

import io.jsonwebtoken.JwtException;

/**
 * Unit tests for {@link JwtService} — pure crypto round-trip, no Spring.
 */
class JwtServiceTest {

    private static final String SECRET = "test-hs256-secret-must-be-at-least-32-bytes-long-abc";
    private static final String OTHER  = "another-hs256-secret-of-sufficient-length-1234567890";

    private JwtService service;

    @BeforeEach
    void setUp() {
        service = new JwtService(new JwtProperties(SECRET, "eazicut-api", Duration.ofMinutes(15)));
    }

    private User user(Role role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("test@example.com");
        u.setEmailLower("test@example.com");
        u.setRole(role);
        u.setEnabled(true);
        return u;
    }

    @Test
    @DisplayName("issue + parse — round-trip surfaces id, email, role")
    void roundTrip() {
        User u = user(Role.CUSTOMER);
        String token = service.issueAccessToken(u);

        ParsedAccessToken parsed = service.parse(token);
        assertThat(parsed.userId()).isEqualTo(u.getId());
        assertThat(parsed.email()).isEqualTo("test@example.com");
        assertThat(parsed.role()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("parse — admin role is preserved (drives ROLE_ADMIN authority downstream)")
    void adminRolePreserved() {
        User admin = user(Role.ADMIN);
        String token = service.issueAccessToken(admin);
        assertThat(service.parse(token).role()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("parse — signature mismatch throws JwtException")
    void wrongSignatureRejected() {
        JwtService alt = new JwtService(new JwtProperties(OTHER, "eazicut-api", Duration.ofMinutes(15)));
        String token = alt.issueAccessToken(user(Role.CUSTOMER));

        assertThatThrownBy(() -> service.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("parse — wrong issuer throws JwtException")
    void wrongIssuerRejected() {
        JwtService alt = new JwtService(new JwtProperties(SECRET, "someone-else", Duration.ofMinutes(15)));
        String token = alt.issueAccessToken(user(Role.CUSTOMER));

        assertThatThrownBy(() -> service.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("parse — token expired more than the leeway throws JwtException")
    void expiredTokenRejected() throws Exception {
        JwtService shortLived = new JwtService(new JwtProperties(SECRET, "eazicut-api", Duration.ofMillis(1)));
        String token = shortLived.issueAccessToken(user(Role.CUSTOMER));
        Thread.sleep(50);
        assertThatThrownBy(() -> shortLived.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("parse — malformed token throws JwtException")
    void malformedRejected() {
        assertThatThrownBy(() -> service.parse("not-a-real-jwt")).isInstanceOf(JwtException.class);
    }
}
