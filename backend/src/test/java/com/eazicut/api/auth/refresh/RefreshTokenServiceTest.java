package com.eazicut.api.auth.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import com.eazicut.api.auth.exception.InvalidCredentialsException;
import com.eazicut.api.users.entity.Role;
import com.eazicut.api.users.entity.User;
import com.eazicut.api.users.repository.UserRepository;

/**
 * Slice tests for {@link RefreshTokenService} + {@link RefreshTokenRepository}.
 *
 * <p>Runs the service against real Flyway-built H2 schema so the V5
 * migration + entity mapping stays honest. {@code @Import} wires the
 * service bean into the slice (it isn't picked up automatically).
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Import(RefreshTokenServiceTest.SliceConfig.class)
@TestPropertySource(properties = "eazicut.jwt.refresh-token-ttl=7d")
class RefreshTokenServiceTest {

    @TestConfiguration
    static class SliceConfig {
        @Bean RefreshTokenService refreshTokenService(RefreshTokenRepository r) {
            RefreshTokenService s = new RefreshTokenService(r);
            ReflectionTestUtils.setField(s, "refreshTokenTtl", Duration.ofDays(7));
            return s;
        }
    }

    @Autowired RefreshTokenService service;
    @Autowired RefreshTokenRepository repository;
    @Autowired UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("refresh-tests@example.com");
        user.setPasswordHash("$2a$10$fakeHash");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        user = userRepository.saveAndFlush(user);
    }

    @Test
    @DisplayName("issue — stores only the SHA-256 hash; the raw token is 43+ chars base64url")
    void issueStoresHashOnly() {
        RefreshTokenService.IssuedToken issued = service.issue(user);

        assertThat(issued.rawToken()).isNotBlank();
        assertThat(issued.rawToken().length()).isGreaterThanOrEqualTo(43); // 32 bytes base64url ≈ 43 chars
        assertThat(repository.findAll()).hasSize(1);

        RefreshToken row = repository.findAll().get(0);
        assertThat(row.getTokenHash()).hasSize(64); // sha-256 hex
        assertThat(row.getTokenHash()).isNotEqualTo(issued.rawToken()); // hash != raw
    }

    @Test
    @DisplayName("rotate — happy path: old row revoked, new one persisted, same user")
    void rotateHappyPath() {
        RefreshTokenService.IssuedToken first = service.issue(user);

        RefreshTokenService.RotationResult rotated = service.rotate(first.rawToken());

        assertThat(rotated.user().getId()).isEqualTo(user.getId());
        assertThat(rotated.next().rawToken()).isNotEqualTo(first.rawToken());
        assertThat(repository.findAll()).hasSize(2);
        // The original row must be tombstoned.
        assertThat(repository.findByTokenHash(RefreshTokenService.sha256(first.rawToken())))
                .isPresent().get().extracting(RefreshToken::getRevokedAt).isNotNull();
    }

    @Test
    @DisplayName("rotate — unknown token → InvalidCredentialsException")
    void rotateUnknown() {
        assertThatThrownBy(() -> service.rotate("never-issued"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("rotate — already-revoked token → InvalidCredentialsException")
    void rotateAlreadyRevoked() {
        RefreshTokenService.IssuedToken first = service.issue(user);
        service.rotate(first.rawToken()); // marks first revoked, mints second

        assertThatThrownBy(() -> service.rotate(first.rawToken()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("rotate — expired token → InvalidCredentialsException")
    void rotateExpired() {
        RefreshToken row = new RefreshToken();
        row.setUser(user);
        row.setTokenHash(RefreshTokenService.sha256("expired-raw"));
        row.setIssuedAt(Instant.now().minus(Duration.ofDays(10)));
        row.setExpiresAt(Instant.now().minus(Duration.ofDays(1)));
        repository.saveAndFlush(row);

        assertThatThrownBy(() -> service.rotate("expired-raw"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("rotate — disabled user mid-session → InvalidCredentialsException")
    void rotateDisabledUser() {
        RefreshTokenService.IssuedToken first = service.issue(user);
        user.setEnabled(false);
        userRepository.saveAndFlush(user);

        assertThatThrownBy(() -> service.rotate(first.rawToken()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("revoke — marks the row revoked; second call is a no-op (idempotent)")
    void revokeIdempotent() {
        RefreshTokenService.IssuedToken issued = service.issue(user);

        service.revoke(issued.rawToken());
        service.revoke(issued.rawToken()); // must not throw

        RefreshToken row = repository.findByTokenHash(RefreshTokenService.sha256(issued.rawToken())).orElseThrow();
        assertThat(row.getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("revoke — unknown token is silently accepted")
    void revokeUnknown() {
        service.revoke("does-not-exist");
        service.revoke(null);
        service.revoke("");
    }

    @Test
    @DisplayName("sha256 — deterministic, 64-char lowercase hex")
    void sha256Shape() {
        String h = RefreshTokenService.sha256("hello");
        assertThat(h).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(RefreshTokenService.sha256("hello")).isEqualTo(h); // deterministic
        assertThat(RefreshTokenService.sha256("hellp")).isNotEqualTo(h); // avalanche
    }

    @Test
    @DisplayName("id — generated on save (UUID column)")
    void idGenerated() {
        service.issue(user);
        UUID id = repository.findAll().get(0).getId();
        assertThat(id).isNotNull();
    }
}
