package com.eazicut.api.users.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.eazicut.api.users.entity.Role;
import com.eazicut.api.users.entity.User;

/**
 * Repository-slice tests for {@link UserRepository}.
 *
 * <p>Same shape as CategoryRepositoryTest / CollectionRepositoryTest —
 * Flyway builds the schema via V4, {@code ddl-auto=validate} guards
 * against entity/schema drift. Verifies:
 *
 * <ul>
 *   <li>The {@code @PrePersist} {@code emailLower} sync fires on save.</li>
 *   <li>Lookup by {@code emailLower} is case-insensitive as advertised.</li>
 *   <li>The V4 unique index on {@code email_lower} rejects a duplicate
 *       insert even when the raw {@code email} differs only in casing.</li>
 * </ul>
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
class UserRepositoryTest {

    @Autowired private UserRepository userRepository;

    private User persist(String email) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXY"); // fake BCrypt
        u.setRole(Role.CUSTOMER);
        u.setEnabled(true);
        return userRepository.saveAndFlush(u);
    }

    @Test
    @DisplayName("@PrePersist syncs email_lower from email (trim + lowercase)")
    void syncEmailLower() {
        User u = persist("  Someone@Eazicut.COM ");
        assertThat(u.getEmailLower()).isEqualTo("someone@eazicut.com");
    }

    @Test
    @DisplayName("findByEmailLower — matches after normalisation")
    void findByEmailLower() {
        persist("Someone@Eazicut.com");
        assertThat(userRepository.findByEmailLower("someone@eazicut.com")).isPresent();
        assertThat(userRepository.findByEmailLower("nope@eazicut.com")).isEmpty();
    }

    @Test
    @DisplayName("existsByEmailLower — case-insensitive presence check")
    void existsByEmailLower() {
        persist("Someone@Eazicut.com");
        assertThat(userRepository.existsByEmailLower("someone@eazicut.com")).isTrue();
        assertThat(userRepository.existsByEmailLower("SOMEONE@EAZICUT.COM".toLowerCase())).isTrue();
        assertThat(userRepository.existsByEmailLower("other@eazicut.com")).isFalse();
    }

    @Test
    @DisplayName("V4 unique index — insert of case-different duplicate is rejected")
    void uniqueIndexOnEmailLower() {
        persist("someone@eazicut.com");
        assertThatThrownBy(() -> persist("Someone@Eazicut.COM"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
