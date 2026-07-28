package com.eazicut.api.auth.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.eazicut.api.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Application service for user registration and login.
 *
 * <p>Registration lives here from Stage 2; login arrives in Stage 4 on
 * the same façade so both write paths share the {@link UserRepository}
 * + {@link PasswordEncoder} plumbing. Refresh + logout land in Stage 5.
 *
 * <p><strong>Login safety — three baked-in mitigations:</strong>
 * <ol>
 *   <li><em>Constant-time behaviour on unknown email.</em> When the
 *       email doesn't resolve to a user, we still call
 *       {@link PasswordEncoder#matches} against a fixed dummy hash so
 *       the response latency is indistinguishable from a "wrong
 *       password on a real user". This closes the timing side-channel
 *       that would otherwise let an attacker enumerate accounts.</li>
 *   <li><em>Uniform error.</em> Every failure — unknown email, wrong
 *       password, disabled account — surfaces the same
 *       {@link InvalidCredentialsException} with the generic message.
 *       No enumeration signal in the response body either.</li>
 *   <li><em>Per-IP + per-email rate limit.</em> The
 *       {@link LoginRateLimiter} is consulted before the password
 *       check and updated after. Five failures on either dimension in
 *       15 minutes blocks further attempts with a 429 + Retry-After.</li>
 * </ol>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    /**
     * BCrypt hash of the constant string "unknown-user" using cost 10.
     * Never matches any real password. Used only for constant-time
     * behaviour when the email lookup returns empty — see class Javadoc.
     */
    private static final String DUMMY_HASH =
            "$2a$10$abcdefghijklmnopqrstuu5aExpjhqIPT7SXNbnHYMzYlwR8xEnXi";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final LoginRateLimiter rateLimiter;
    private final RefreshTokenService refreshTokenService;

    // ---------------------------------------------------------------------
    // Register (Stage 2)
    // ---------------------------------------------------------------------

    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim();
        String emailLower = email.toLowerCase();

        if (userRepository.existsByEmailLower(emailLower)) {
            throw new DuplicateEmailException(email);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(trimToNull(request.displayName()));
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);

        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    // ---------------------------------------------------------------------
    // Login (Stage 4)
    // ---------------------------------------------------------------------

    /**
     * Authenticate a customer or admin and mint a short-lived JWT
     * access token.
     *
     * @param request DTO with email + password
     * @param ip      caller IP, used by the rate limiter as one of the
     *                two counted dimensions
     */
    public LoginResponse login(LoginRequest request, String ip) {
        String email = request.email().trim();
        String emailLower = email.toLowerCase();

        rateLimiter.assertAllowed(ip, emailLower);

        Optional<User> maybeUser = userRepository.findByEmailLower(emailLower);
        String hash = maybeUser.map(User::getPasswordHash).orElse(DUMMY_HASH);

        boolean passwordOk = passwordEncoder.matches(request.password(), hash);
        boolean userOk = maybeUser.map(User::isEnabled).orElse(false);

        if (!passwordOk || !userOk) {
            rateLimiter.recordFailure(ip, emailLower);
            throw new InvalidCredentialsException();
        }

        rateLimiter.recordSuccess(ip, emailLower);

        User user = maybeUser.get();
        String accessToken = jwtService.issueAccessToken(user);
        IssuedToken refresh = refreshTokenService.issue(user);
        return new LoginResponse(
                accessToken,
                jwtProperties.accessTokenTtl().toSeconds(),
                userMapper.toResponse(user),
                refresh.rawToken()
        );
    }

    // ---------------------------------------------------------------------
    // Refresh (Stage 5)
    // ---------------------------------------------------------------------

    /**
     * Consume the given raw refresh token, rotate it, and mint a fresh
     * access token for the same user. The returned {@link LoginResponse}
     * shape is identical to a full login so the frontend has one code
     * path for both.
     *
     * @throws InvalidCredentialsException if the refresh token is
     *         unknown, revoked, expired, or the user is disabled.
     */
    public LoginResponse refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidCredentialsException();
        }
        RotationResult rotated = refreshTokenService.rotate(rawRefreshToken);
        String accessToken = jwtService.issueAccessToken(rotated.user());
        return new LoginResponse(
                accessToken,
                jwtProperties.accessTokenTtl().toSeconds(),
                userMapper.toResponse(rotated.user()),
                rotated.next().rawToken()
        );
    }

    /**
     * Revoke the given raw refresh token. Idempotent — an unknown or
     * already-revoked token is silently accepted, so double-logout
     * doesn't error. See {@link RefreshTokenService#revoke}.
     */
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    /**
     * Convenience for the controller so it can set the refresh cookie's
     * {@code Max-Age} without a second dependency wire.
     */
    public java.time.Duration refreshTokenTtl() {
        return refreshTokenService.refreshTokenTtl();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
