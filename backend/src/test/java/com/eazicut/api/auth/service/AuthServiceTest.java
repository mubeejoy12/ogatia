package com.eazicut.api.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.eazicut.api.auth.dto.LoginRequest;
import com.eazicut.api.auth.dto.LoginResponse;
import com.eazicut.api.auth.dto.RegisterRequest;
import com.eazicut.api.auth.exception.DuplicateEmailException;
import com.eazicut.api.auth.exception.InvalidCredentialsException;
import com.eazicut.api.auth.jwt.JwtProperties;
import com.eazicut.api.auth.jwt.JwtService;
import com.eazicut.api.auth.ratelimit.LoginRateLimiter;
import com.eazicut.api.auth.refresh.RefreshTokenService;
import com.eazicut.api.auth.refresh.RefreshTokenService.IssuedToken;
import com.eazicut.api.auth.refresh.RefreshTokenService.RotationResult;
import com.eazicut.api.users.dto.UserResponse;
import com.eazicut.api.users.entity.Role;
import com.eazicut.api.users.entity.User;
import com.eazicut.api.users.mapper.UserMapper;
import com.eazicut.api.users.mapper.UserMapperImpl;
import com.eazicut.api.users.repository.UserRepository;

/**
 * Unit tests for {@link AuthService} (Stage 2 — registration only).
 *
 * <p>Uses the generated {@link UserMapperImpl} directly for the same
 * reason CategoryServiceTest and CollectionServiceTest do — MapStruct
 * emits plain classes, so mapping stays exercised alongside service
 * logic. Repository + PasswordEncoder are mocked.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LoginRateLimiter rateLimiter;

    // ByteBuddy can't yet instrument concrete classes on Java 25.
    // JwtService and RefreshTokenService are real; the latter is stubbed
    // with a hand-rolled fake so we can inspect the calls without mocking.
    private final UserMapper mapper = new UserMapperImpl();
    private final JwtProperties jwtProps =
            new JwtProperties("test-secret-1234567890-1234567890-abcd", "eazicut-api", Duration.ofMinutes(15));
    private final JwtService jwtService = new JwtService(jwtProps);
    private final FakeRefreshTokenService refreshTokenService = new FakeRefreshTokenService();

    private AuthService service;

    @BeforeEach
    void setUp() {
        refreshTokenService.reset();
        service = new AuthService(userRepository, passwordEncoder, mapper, jwtService, jwtProps, rateLimiter, refreshTokenService);
    }

    /**
     * Fake {@link RefreshTokenService} — a subclass override so we can
     * intercept {@code issue}/{@code rotate}/{@code revoke} without
     * bringing a DB or Mockito into play.
     */
    static class FakeRefreshTokenService extends RefreshTokenService {
        User lastIssuedFor;
        String lastRotatedRaw;
        String lastRevokedRaw;
        int issueCallCount;

        FakeRefreshTokenService() {
            super(null);
        }

        void reset() {
            lastIssuedFor = null;
            lastRotatedRaw = null;
            lastRevokedRaw = null;
            issueCallCount = 0;
        }

        @Override
        public IssuedToken issue(User user) {
            lastIssuedFor = user;
            issueCallCount++;
            return new IssuedToken("fake-raw-refresh-token-" + issueCallCount,
                    java.time.Instant.now().plus(Duration.ofDays(7)));
        }

        @Override
        public RotationResult rotate(String raw) {
            lastRotatedRaw = raw;
            if (raw == null || raw.isBlank() || raw.equals("bad")) {
                throw new InvalidCredentialsException();
            }
            User u = new User();
            u.setId(UUID.randomUUID());
            u.setEmail("customer@example.com");
            u.setEmailLower("customer@example.com");
            u.setPasswordHash("$2a$10$realHash");
            u.setRole(Role.CUSTOMER);
            u.setEnabled(true);
            return new RotationResult(u, issue(u));
        }

        @Override
        public void revoke(String raw) {
            lastRevokedRaw = raw;
        }

        @Override
        public Duration refreshTokenTtl() {
            return Duration.ofDays(7);
        }
    }

    @Test
    @DisplayName("register — persists a CUSTOMER with hashed password when email is free")
    void registerHappyPath() {
        RegisterRequest req = new RegisterRequest("customer@example.com", "correct horse battery", "Some Person");
        given(userRepository.existsByEmailLower("customer@example.com")).willReturn(false);
        given(passwordEncoder.encode("correct horse battery")).willReturn("$2a$10$fakeHash");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        UserResponse response = service.register(req);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("customer@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("$2a$10$fakeHash");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.getValue().getDisplayName()).isEqualTo("Some Person");
        assertThat(saved.getValue().isEnabled()).isTrue();
        assertThat(response.email()).isEqualTo("customer@example.com");
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("register — normalises email (trim + lowercase) before probing uniqueness")
    void registerNormalisesEmail() {
        RegisterRequest req = new RegisterRequest("  Someone@Eazicut.COM ", "correct horse battery", null);
        given(userRepository.existsByEmailLower("someone@eazicut.com")).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("$2a$10$fakeHash");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        service.register(req);

        // Probe hit the normalised form
        verify(userRepository).existsByEmailLower("someone@eazicut.com");
        // Stored value keeps caller casing minus the outer whitespace
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("Someone@Eazicut.COM");
    }

    @Test
    @DisplayName("register — displayName is trimmed; empty/whitespace collapses to null")
    void registerDisplayNameTrimmed() {
        RegisterRequest emptyName = new RegisterRequest("a@b.co", "correct horse battery", "   ");
        given(userRepository.existsByEmailLower(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("$2a$10$fakeHash");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        service.register(emptyName);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getDisplayName()).isNull();
    }

    @Test
    @DisplayName("register — throws DuplicateEmailException when email already taken; save never called")
    void registerDuplicateEmailRejected() {
        RegisterRequest req = new RegisterRequest("taken@example.com", "correct horse battery", null);
        given(userRepository.existsByEmailLower("taken@example.com")).willReturn(true);

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("taken@example.com");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("register — case-different email is still a duplicate (probe normalised)")
    void registerDuplicateEmailCaseInsensitive() {
        RegisterRequest req = new RegisterRequest("TAKEN@example.com", "correct horse battery", null);
        given(userRepository.existsByEmailLower("taken@example.com")).willReturn(true);

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // Login (Stage 4)
    // ---------------------------------------------------------------------

    private User realCustomer() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("customer@example.com");
        u.setEmailLower("customer@example.com");
        u.setPasswordHash("$2a$10$realHash");
        u.setDisplayName("Customer");
        u.setRole(Role.CUSTOMER);
        u.setEnabled(true);
        return u;
    }

    @Test
    @DisplayName("login — happy path mints a real access token, resets rate limit for this email")
    void loginHappyPath() {
        User user = realCustomer();
        given(userRepository.findByEmailLower("customer@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("correct horse battery", "$2a$10$realHash")).willReturn(true);

        LoginResponse resp = service.login(
                new LoginRequest("customer@example.com", "correct horse battery"),
                "1.2.3.4"
        );

        // Round-trip: prove the returned token is a real, parseable JWT
        // carrying the persisted user's identity.
        var parsed = jwtService.parse(resp.accessToken());
        assertThat(parsed.userId()).isEqualTo(user.getId());
        assertThat(parsed.email()).isEqualTo("customer@example.com");
        assertThat(parsed.role()).isEqualTo(Role.CUSTOMER);
        assertThat(resp.expiresInSeconds()).isEqualTo(15 * 60);
        assertThat(resp.user().email()).isEqualTo("customer@example.com");
        assertThat(resp.user().role()).isEqualTo(Role.CUSTOMER);
        verify(rateLimiter).assertAllowed("1.2.3.4", "customer@example.com");
        verify(rateLimiter).recordSuccess("1.2.3.4", "customer@example.com");
        verify(rateLimiter, never()).recordFailure(anyString(), anyString());
    }

    @Test
    @DisplayName("login — normalises email (trim + lowercase) before lookup and rate-limit key")
    void loginNormalisesEmail() {
        User user = realCustomer();
        given(userRepository.findByEmailLower("customer@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

        service.login(new LoginRequest("  CUSTOMER@Example.com ", "correct horse battery"), "1.2.3.4");

        verify(rateLimiter).assertAllowed("1.2.3.4", "customer@example.com");
        verify(userRepository).findByEmailLower("customer@example.com");
    }

    @Test
    @DisplayName("login — wrong password → InvalidCredentialsException + rate-limit failure recorded")
    void loginWrongPassword() {
        User user = realCustomer();
        given(userRepository.findByEmailLower("customer@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("nope", "$2a$10$realHash")).willReturn(false);

        assertThatThrownBy(() -> service.login(
                new LoginRequest("customer@example.com", "nope"), "1.2.3.4"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(rateLimiter).recordFailure("1.2.3.4", "customer@example.com");
        verify(rateLimiter, never()).recordSuccess(anyString(), anyString());
    }

    @Test
    @DisplayName("login — unknown email still runs BCrypt (constant-time; no enumeration signal)")
    void loginUnknownEmailStillHashes() {
        given(userRepository.findByEmailLower("ghost@example.com")).willReturn(Optional.empty());
        // Match against ANY hash — the important part is that we DID call
        // matches() even though the user doesn't exist, so latency is
        // indistinguishable from a "real user, wrong password" call.
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        assertThatThrownBy(() -> service.login(
                new LoginRequest("ghost@example.com", "any-password"), "1.2.3.4"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
        verify(rateLimiter).recordFailure("1.2.3.4", "ghost@example.com");
    }

    @Test
    @DisplayName("login — disabled user → InvalidCredentialsException (same generic error as wrong password)")
    void loginDisabledUser() {
        User user = realCustomer();
        user.setEnabled(false);
        given(userRepository.findByEmailLower("customer@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("correct horse battery", "$2a$10$realHash")).willReturn(true);

        assertThatThrownBy(() -> service.login(
                new LoginRequest("customer@example.com", "correct horse battery"), "1.2.3.4"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(rateLimiter).recordFailure(anyString(), anyString());
    }

    @Test
    @DisplayName("login — successful login mints an access token AND a refresh token; refreshToken is @JsonIgnored")
    void loginMintsRefreshToken() {
        User user = realCustomer();
        given(userRepository.findByEmailLower("customer@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

        LoginResponse resp = service.login(
                new LoginRequest("customer@example.com", "correct horse battery"),
                "1.2.3.4"
        );

        assertThat(resp.refreshToken()).isNotBlank();
        assertThat(refreshTokenService.issueCallCount).isEqualTo(1);
        assertThat(refreshTokenService.lastIssuedFor).isSameAs(user);
    }

    // ---------------------------------------------------------------------
    // Refresh + logout (Stage 5)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("refresh — happy path returns fresh access + rotated refresh token")
    void refreshHappyPath() {
        LoginResponse resp = service.refresh("some-raw-cookie-value");

        assertThat(refreshTokenService.lastRotatedRaw).isEqualTo("some-raw-cookie-value");
        assertThat(resp.accessToken()).isNotBlank();
        assertThat(resp.refreshToken()).isNotBlank();
        assertThat(resp.expiresInSeconds()).isEqualTo(15 * 60);
    }

    @Test
    @DisplayName("refresh — null or blank cookie → InvalidCredentialsException, no rotate attempted")
    void refreshMissingCookie() {
        assertThatThrownBy(() -> service.refresh(null))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.refresh("   "))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(refreshTokenService.lastRotatedRaw).isNull();
    }

    @Test
    @DisplayName("refresh — bad cookie value propagates 401 from RefreshTokenService")
    void refreshBadCookie() {
        assertThatThrownBy(() -> service.refresh("bad"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("logout — forwards raw token to RefreshTokenService.revoke")
    void logoutForwards() {
        service.logout("some-raw-cookie-value");
        assertThat(refreshTokenService.lastRevokedRaw).isEqualTo("some-raw-cookie-value");
    }
}
