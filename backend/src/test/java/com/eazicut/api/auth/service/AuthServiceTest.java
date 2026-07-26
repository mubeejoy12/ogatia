package com.eazicut.api.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.eazicut.api.auth.dto.RegisterRequest;
import com.eazicut.api.auth.exception.DuplicateEmailException;
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

    private final UserMapper mapper = new UserMapperImpl();

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder, mapper);
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
}
