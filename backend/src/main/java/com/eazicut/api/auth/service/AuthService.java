package com.eazicut.api.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eazicut.api.auth.dto.RegisterRequest;
import com.eazicut.api.auth.exception.DuplicateEmailException;
import com.eazicut.api.users.dto.UserResponse;
import com.eazicut.api.users.entity.Role;
import com.eazicut.api.users.entity.User;
import com.eazicut.api.users.mapper.UserMapper;
import com.eazicut.api.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Application service for user registration (Stage 2).
 *
 * <p>Login, refresh, and logout land in Stages 4/5 on this same service;
 * they will share the {@link UserRepository} + {@link PasswordEncoder}
 * plumbing wired here so all password work stays behind a single façade.
 *
 * <p><strong>Uniqueness — two layers</strong>, following the pattern
 * B003 established:
 * <ol>
 *   <li>Service probe ({@code existsByEmailLower}) — turns most
 *       collisions into a clean 409 with a helpful message before the
 *       INSERT is attempted.</li>
 *   <li>DB backstop — {@code ux_user_email_lower} (V4 migration) —
 *       catches concurrent registrations that slip past the probe. The
 *       resulting {@code DataIntegrityViolationException} is mapped to
 *       409 by {@code GlobalExceptionHandler}.</li>
 * </ol>
 *
 * <p><strong>Email normalisation.</strong> The service trims the email
 * once, stores the caller's casing in {@code email}, and lets the
 * entity's {@code @PrePersist} populate {@code email_lower}. The
 * uniqueness probe uses the same lower-cased form so probe and INSERT
 * see identical shapes.
 *
 * <p><strong>Password.</strong> The raw string never leaves this
 * method — {@link PasswordEncoder#encode} runs before the entity is
 * saved and only the BCrypt hash reaches the DB.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

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

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
