package com.eazicut.api.users.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.eazicut.api.users.entity.Role;
import com.eazicut.api.users.entity.User;
import com.eazicut.api.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Dev-only, idempotent admin bootstrap.
 *
 * <p><strong>Why this exists.</strong> Retires the Spring Boot in-memory
 * user store ({@code spring.security.user.*}) which was fine as a B001
 * scaffold but a footgun in prod (accidentally set env vars would seed
 * a live admin without an audit trail). Every environment — dev, test,
 * prod — now authenticates against the same {@code users} table, using
 * the same {@code UserDetailsService} and the same {@link PasswordEncoder}.
 *
 * <p><strong>Configuration.</strong> Reads
 * {@code eazicut.dev-admin.email} and {@code eazicut.dev-admin.password}
 * — bindable to env vars {@code EAZICUT_DEV_ADMIN_EMAIL} and
 * {@code EAZICUT_DEV_ADMIN_PASSWORD}. In {@code application-dev.yml}
 * they default to {@code admin@eazicut.local / admin} so the B002/B003
 * curl flows continue to work with no env setup. Override to run a
 * local instance with your own credentials without editing YAML.
 *
 * <p><strong>Idempotency.</strong> Guarded by
 * {@code existsByEmailLower} — re-running is a no-op on the already-
 * seeded row. The row is never overwritten; rotating the dev admin
 * password means deleting the row (or updating it manually via the
 * H2 console) and re-booting.
 *
 * <p><strong>Never runs outside dev.</strong> {@link Profile @Profile("dev")}
 * — the bean isn't registered in prod or test. If we ever want a
 * bootstrap admin in prod, it will be an explicit one-shot ops job,
 * not a runtime side effect.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevAdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevAdminSeeder.class);

    @Value("${eazicut.dev-admin.email}")
    private String adminEmail;

    @Value("${eazicut.dev-admin.password}")
    private String adminPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String key = adminEmail.trim().toLowerCase();
        if (userRepository.existsByEmailLower(key)) {
            log.info("[dev-admin-seed] admin '{}' already present — no changes", key);
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail.trim());
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setDisplayName("Atelier Admin");
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);

        log.info("[dev-admin-seed] created admin user '{}' (dev profile only)", adminEmail);
    }
}
