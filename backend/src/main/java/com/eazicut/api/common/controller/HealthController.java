package com.eazicut.api.common.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight health endpoint the frontend can hit to confirm the API
 * is reachable — deliberately separate from Spring Boot Actuator's
 * {@code /actuator/health}, which is intended for orchestration probes
 * (Kubernetes liveness/readiness, load balancers).
 *
 * <p>Responds with a small, uncached JSON body — no downstream dependency
 * checks are performed here so it always answers even if the database
 * is temporarily unavailable.
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @Value("${spring.application.name:eazi-cut-api}")
    private String serviceName;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "status", "OK",
                "service", serviceName,
                "profile", activeProfile,
                "timestamp", Instant.now().toString()
        );
    }
}
