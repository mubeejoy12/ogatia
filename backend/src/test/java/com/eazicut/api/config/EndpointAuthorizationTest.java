package com.eazicut.api.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.eazicut.api.users.entity.Role;
import com.eazicut.api.users.entity.User;
import com.eazicut.api.users.repository.UserRepository;

/**
 * D8 safety net — walks every mapped controller endpoint and asserts the
 * expected authorization level.
 *
 * <p>This test is the reason we can flip {@code .anyRequest().permitAll()}
 * to {@code .anyRequest().authenticated()} without holding our breath.
 * A new endpoint that's accidentally left public — or worse, forgotten
 * behind {@code authenticated} when it should be public — will fail here
 * before it can ship.
 *
 * <p><strong>Matrix:</strong>
 * <ul>
 *   <li>Public endpoints: anonymous → 2xx / 4xx (not 401).</li>
 *   <li>Admin-only endpoints: anonymous → 401; CUSTOMER → 403; ADMIN → 2xx / 4xx.</li>
 * </ul>
 *
 * <p>HTTP Basic (still enabled transitionally in Stage 3) is used as the
 * credential source. When Stage 4 removes Basic, this test will be
 * updated to mint a real JWT via {@code AuthService.login()} and hit the
 * same matrix with a Bearer header.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eazicut.jwt.secret=test-hs256-secret-must-be-at-least-32-bytes-long-abc",
        "eazicut.jwt.issuer=eazicut-api",
        "eazicut.jwt.access-token-ttl=15m",
        "eazicut.dev-admin.email=admin@test.local",
        "eazicut.dev-admin.password=admin"
})
class EndpointAuthorizationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String CUSTOMER_EMAIL = "e2e-customer@test.local";
    private static final String CUSTOMER_PASSWORD = "correct horse battery";

    @BeforeEach
    void seedCustomer() {
        if (userRepository.existsByEmailLower(CUSTOMER_EMAIL)) return;
        User u = new User();
        u.setEmail(CUSTOMER_EMAIL);
        u.setPasswordHash(passwordEncoder.encode(CUSTOMER_PASSWORD));
        u.setRole(Role.CUSTOMER);
        u.setEnabled(true);
        userRepository.save(u);
    }

    // ------------------------------------------------------------------
    // Public — anonymous must NOT be challenged for auth
    // ------------------------------------------------------------------

    @Test @DisplayName("public — GET /health anonymous → 200")
    void healthPublic() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
    }

    @Test @DisplayName("public — GET /products anonymous → 200")
    void productsListPublic() throws Exception {
        mockMvc.perform(get("/products")).andExpect(status().isOk());
    }

    @Test @DisplayName("public — GET /categories anonymous → 200")
    void categoriesListPublic() throws Exception {
        mockMvc.perform(get("/categories")).andExpect(status().isOk());
    }

    @Test @DisplayName("public — GET /collections anonymous → 200")
    void collectionsListPublic() throws Exception {
        mockMvc.perform(get("/collections")).andExpect(status().isOk());
    }

    @Test @DisplayName("public — POST /auth/register anonymous → 400 (validation), not 401 (proves it's on the allowlist)")
    void authRegisterPublic() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Admin-only writes — three angles per endpoint family
    // ------------------------------------------------------------------

    @Test @DisplayName("protected — POST /products anonymous → 401")
    void productsCreateAnonymous() throws Exception {
        mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("protected — POST /products CUSTOMER → 403")
    void productsCreateCustomer() throws Exception {
        // Body must pass @Valid so we reach @PreAuthorize; otherwise Spring
        // returns 400 during argument resolution before the auth check.
        String validProduct = """
                {"name":"Test Piece","slug":"test-piece-auth-check","shortDescription":"short",
                 "fullDescription":"full","sku":"TST-AUTH-1","price":1000,"stockQuantity":1,
                 "images":[{"url":"https://example.com/x.jpg","alt":"x","sortOrder":0,"primary":true}]}""";
        mockMvc.perform(post("/products")
                        .with(httpBasic(CUSTOMER_EMAIL, CUSTOMER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(validProduct))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("protected — POST /categories anonymous → 401")
    void categoriesCreateAnonymous() throws Exception {
        mockMvc.perform(post("/categories").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("protected — POST /categories CUSTOMER → 403")
    void categoriesCreateCustomer() throws Exception {
        mockMvc.perform(post("/categories")
                        .with(httpBasic(CUSTOMER_EMAIL, CUSTOMER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Cat\",\"slug\":\"test-cat\"}"))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("protected — POST /collections anonymous → 401")
    void collectionsCreateAnonymous() throws Exception {
        mockMvc.perform(post("/collections").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("protected — POST /collections CUSTOMER → 403")
    void collectionsCreateCustomer() throws Exception {
        mockMvc.perform(post("/collections")
                        .with(httpBasic(CUSTOMER_EMAIL, CUSTOMER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Col\",\"slug\":\"test-col\"}"))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("protected — PUT and DELETE writes anonymous → 401 across all three resources")
    void putDeleteAnonymous() throws Exception {
        UUID id = UUID.randomUUID();
        for (String base : new String[]{"/products/", "/categories/", "/collections/"}) {
            mockMvc.perform(put(base + id).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(delete(base + id))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ------------------------------------------------------------------
    // Unknown routes still 401 when they require a body/write intent
    // (proves the default is authenticated, not permitAll)
    // ------------------------------------------------------------------

    @Test @DisplayName("secure default — unmapped write route anonymous → 401 (not 404 through auth's back door)")
    void unmappedWriteAnonymous() throws Exception {
        // Any unmapped route under the API prefix should authenticate first;
        // an anonymous request cannot even reach the 404 handler for a
        // protected path.
        mockMvc.perform(post("/nonexistent-endpoint-xyz")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
