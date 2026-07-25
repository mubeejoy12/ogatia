package com.eazicut.api.users.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.eazicut.api.users.entity.Role;
import com.eazicut.api.users.entity.User;
import com.eazicut.api.users.repository.UserRepository;

/**
 * Unit tests for {@link JpaUserDetailsService}.
 *
 * <p>Pure Mockito — the repository is mocked. Focus is on the boundary
 * contract: input normalisation (trim + lowercase) and the missing-user
 * translation to {@link UsernameNotFoundException}.
 */
@ExtendWith(MockitoExtension.class)
class JpaUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private JpaUserDetailsService service;

    @Test
    @DisplayName("loadUserByUsername — normalises input before querying")
    void normalisesInput() {
        User u = new User();
        u.setEmail("Someone@Eazicut.com");
        u.setEmailLower("someone@eazicut.com");
        u.setPasswordHash("hash");
        u.setRole(Role.CUSTOMER);
        u.setEnabled(true);
        given(userRepository.findByEmailLower("someone@eazicut.com")).willReturn(Optional.of(u));

        var details = service.loadUserByUsername("  Someone@Eazicut.COM ");

        assertThat(details.getUsername()).isEqualTo("someone@eazicut.com");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_CUSTOMER");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("loadUserByUsername — unknown email → UsernameNotFoundException")
    void unknownEmail() {
        given(userRepository.findByEmailLower("ghost@eazicut.local")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost@eazicut.local"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("loadUserByUsername — admin user surfaces ROLE_ADMIN")
    void adminAuthority() {
        User admin = new User();
        admin.setEmail("admin@eazicut.local");
        admin.setEmailLower("admin@eazicut.local");
        admin.setPasswordHash("hash");
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        given(userRepository.findByEmailLower("admin@eazicut.local")).willReturn(Optional.of(admin));

        var details = service.loadUserByUsername("admin@eazicut.local");

        assertThat(details.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }
}
