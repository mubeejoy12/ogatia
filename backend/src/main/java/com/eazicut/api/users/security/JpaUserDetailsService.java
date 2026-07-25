package com.eazicut.api.users.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eazicut.api.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * DB-backed {@link UserDetailsService} — Spring Security's lookup hook
 * during authentication.
 *
 * <p>Normalises the incoming identifier ({@code trim().toLowerCase()})
 * before hitting the repository so callers can pass "Admin@Eazi.com" or
 * "admin@eazi.com" and land the same row. The lookup itself goes through
 * {@code findByEmailLower} which is served by the
 * {@code ux_user_email_lower} unique index.
 *
 * <p>{@link Transactional} with {@code readOnly = true} — pure read.
 */
@Service
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String key = username == null ? "" : username.trim().toLowerCase();
        return userRepository.findByEmailLower(key)
                .map(UserDetailsAdapter::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user with the supplied credentials."));
    }
}
