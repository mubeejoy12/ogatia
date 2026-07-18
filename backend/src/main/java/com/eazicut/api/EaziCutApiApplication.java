package com.eazicut.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Eazi Cut backend API.
 *
 * <p>Boots the Spring container, exposes REST endpoints under the base
 * path <code>/api/v1</code> (see {@code server.servlet.context-path}
 * in {@code application.yml}), and picks up profile-specific
 * configuration via {@code spring.profiles.active}.
 *
 * <p>Feature packages are introduced one at a time as milestones ship —
 * this scaffold contains only foundation code.
 */
@SpringBootApplication
public class EaziCutApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EaziCutApiApplication.class, args);
    }
}
