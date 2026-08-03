package com.eazicut.api.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.eazicut.api.auth.jwt.JwtService;
import com.eazicut.api.users.entity.Role;
import com.eazicut.api.users.entity.User;
import com.eazicut.api.users.repository.UserRepository;

/**
 * D8 safety net — walks every mapped controller endpoint and asserts the
 * expected authorization level.
 *
 * <p>Post-Stage-4 the wire credential is Bearer JWT only (HTTP Basic
 * has been removed from the filter chain). This test mints tokens
 * directly via {@link JwtService} for the seeded users and attaches
 * them via {@code Authorization} headers.
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
    @Autowired JwtService jwtService;

    private static final String CUSTOMER_EMAIL = "e2e-customer@test.local";
    private static final String CUSTOMER_PASSWORD = "correct horse battery";

    private User customer;

    @BeforeEach
    void seedCustomer() {
        customer = userRepository.findByEmailLower(CUSTOMER_EMAIL).orElseGet(() -> {
            User u = new User();
            u.setEmail(CUSTOMER_EMAIL);
            u.setPasswordHash(passwordEncoder.encode(CUSTOMER_PASSWORD));
            u.setRole(Role.CUSTOMER);
            u.setEnabled(true);
            return userRepository.save(u);
        });
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.issueAccessToken(user);
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

    @Test @DisplayName("public — POST /auth/login anonymous → 400 (validation), not 401 (proves it's on the allowlist)")
    void authLoginPublic() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
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

    @Test @DisplayName("protected — POST /products CUSTOMER (Bearer) → 403")
    void productsCreateCustomer() throws Exception {
        String validProduct = """
                {"name":"Test Piece","slug":"test-piece-auth-check","shortDescription":"short",
                 "fullDescription":"full","sku":"TST-AUTH-1","price":1000,"stockQuantity":1,
                 "images":[{"url":"https://example.com/x.jpg","alt":"x","sortOrder":0,"primary":true}]}""";
        mockMvc.perform(post("/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(validProduct))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("protected — POST /categories anonymous → 401")
    void categoriesCreateAnonymous() throws Exception {
        mockMvc.perform(post("/categories").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("protected — POST /categories CUSTOMER (Bearer) → 403")
    void categoriesCreateCustomer() throws Exception {
        mockMvc.perform(post("/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Cat\",\"slug\":\"test-cat\"}"))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("protected — POST /collections anonymous → 401")
    void collectionsCreateAnonymous() throws Exception {
        mockMvc.perform(post("/collections").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("protected — POST /collections CUSTOMER (Bearer) → 403")
    void collectionsCreateCustomer() throws Exception {
        mockMvc.perform(post("/collections")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer))
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

    @Test @DisplayName("protected — malformed Bearer → 401 (not 500)")
    void malformedBearerRejected() throws Exception {
        mockMvc.perform(post("/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.real.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"slug\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // /auth/me — the first authenticated-only endpoint (Stage 6)
    // ------------------------------------------------------------------

    @Test @DisplayName("auth — GET /auth/me anonymous → 401")
    void meAnonymous() throws Exception {
        mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("auth — GET /auth/me with a valid Bearer returns the caller's profile")
    void meAuthenticated() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(CUSTOMER_EMAIL)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"role\":\"CUSTOMER\"")))
                // Never leaks the password hash or the email_lower detail
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("passwordHash"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("emailLower"))));
    }

    @Test @DisplayName("auth — GET /auth/me with an invalid Bearer → 401")
    void meBadBearer() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.real.jwt"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // /cart/** — the first fully-authenticated CRUD surface (B005 Stage 2)
    // ------------------------------------------------------------------

    @Test @DisplayName("cart — GET /cart anonymous → 401")
    void cartGetAnonymous() throws Exception {
        mockMvc.perform(get("/cart")).andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("cart — POST /cart/items anonymous → 401")
    void cartAddAnonymous() throws Exception {
        mockMvc.perform(post("/cart/items").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("cart — PATCH /cart/items/{id} anonymous → 401")
    void cartPatchAnonymous() throws Exception {
        mockMvc.perform(put("/cart/items/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("cart — DELETE /cart/items/{id} anonymous → 401")
    void cartDeleteItemAnonymous() throws Exception {
        mockMvc.perform(delete("/cart/items/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("cart — DELETE /cart anonymous → 401")
    void cartClearAnonymous() throws Exception {
        mockMvc.perform(delete("/cart")).andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("cart — GET /cart with valid CUSTOMER Bearer → 200 (lazy-creates)")
    void cartGetAuthenticated() throws Exception {
        mockMvc.perform(get("/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("cart — POST /cart/merge anonymous → 401")
    void cartMergeAnonymous() throws Exception {
        mockMvc.perform(post("/cart/merge").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("cart — POST /cart/merge with valid CUSTOMER Bearer (empty payload) → 200")
    void cartMergeAuthenticated() throws Exception {
        mockMvc.perform(post("/cart/merge")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // /orders/** — customer-facing CRUD surface (B006 Stage 3)
    // ------------------------------------------------------------------

    @Test @DisplayName("orders — GET /orders anonymous → 401")
    void ordersListAnonymous() throws Exception {
        mockMvc.perform(get("/orders")).andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("orders — POST /orders anonymous → 401")
    void ordersCreateAnonymous() throws Exception {
        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("orders — GET /orders/{id} anonymous → 401")
    void ordersGetByIdAnonymous() throws Exception {
        mockMvc.perform(get("/orders/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("orders — GET /orders/reference/{ref} anonymous → 401")
    void ordersGetByReferenceAnonymous() throws Exception {
        mockMvc.perform(get("/orders/reference/EAZI-x-y"))
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("orders — GET /orders with valid CUSTOMER Bearer → 200 (empty page)")
    void ordersListAuthenticated() throws Exception {
        mockMvc.perform(get("/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("orders — GET /orders/{unknown-id} with valid Bearer → 404 (not 401 / not 403)")
    void ordersGetUnknownId() throws Exception {
        mockMvc.perform(get("/orders/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isNotFound());
    }

    @Test @DisplayName("orders — POST /orders WITHOUT Idempotency-Key header → 400 (missing_idempotency_key)")
    void ordersCreateMissingKey() throws Exception {
        mockMvc.perform(post("/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deliveryMethodId":"lagos-standard",
                                 "shippingAddress":{"fullName":"C","email":"c@ex.com","phone":"+2340",
                                                    "addressLine1":"1","city":"Lagos","country":"Nigeria"},
                                 "expectedTotal":8000}"""))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Unknown routes still 401 when they require a body/write intent
    // (proves the default is authenticated, not permitAll)
    // ------------------------------------------------------------------

    @Test @DisplayName("secure default — unmapped write route anonymous → 401 (not 404 through auth's back door)")
    void unmappedWriteAnonymous() throws Exception {
        mockMvc.perform(post("/nonexistent-endpoint-xyz")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
