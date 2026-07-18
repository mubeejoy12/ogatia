package com.eazicut.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Smoke test — proves the Spring context boots, autoconfiguration
 * resolves, and the {@code /api/v1/health} endpoint returns 200.
 *
 * Uses the default (dev) profile so H2 stands in for a real database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EaziCutApiApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void contextLoads() {
        // The @SpringBootTest fixture fails the test if the context can't start.
    }

    @Test
    void healthEndpointReturnsOk() {
        ResponseEntity<String> response = rest.getForEntity(
                "http://localhost:" + port + "/api/v1/health", String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"OK\"");
        assertThat(response.getBody()).contains("\"service\":\"eazi-cut-api\"");
    }
}
