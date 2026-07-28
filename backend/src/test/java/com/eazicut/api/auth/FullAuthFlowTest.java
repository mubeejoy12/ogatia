package com.eazicut.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.eazicut.api.auth.refresh.RefreshCookies;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Full-flow end-to-end sanity check of the B004 auth loop.
 *
 * <p>Register → login → hit protected endpoint with Bearer → GET /me
 * → refresh → repeat with new Bearer → logout → confirm old refresh
 * is dead. If any commit ever regresses one of these transitions,
 * this test flips red and the CI pipeline stops it before the
 * frontend team notices.
 *
 * <p>Distinct from {@code EndpointAuthorizationTest}, which is the
 * flat "every endpoint has the right access level" matrix. This one
 * verifies the transitions between the endpoints work as advertised.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eazicut.jwt.secret=test-hs256-secret-must-be-at-least-32-bytes-long-abc",
        "eazicut.jwt.issuer=eazicut-api",
        "eazicut.jwt.access-token-ttl=15m",
        "eazicut.jwt.refresh-token-ttl=7d",
        "eazicut.dev-admin.email=admin@test.local",
        "eazicut.dev-admin.password=admin"
})
class FullAuthFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper json;

    @Test
    @DisplayName("full loop — register → login → me → refresh → me → logout → dead refresh")
    void fullLoop() throws Exception {
        String email = "fullflow@test.local";
        String password = "correct horse battery";

        // 1. Register
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","displayName":"Full Flow"}
                                """.formatted(email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.email").value(email));

        // 2. Login → returns access token + sets refresh cookie
        MvcResult login = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist()) // @JsonIgnore
                .andExpect(cookie().exists(RefreshCookies.COOKIE_NAME))
                .andExpect(cookie().httpOnly(RefreshCookies.COOKIE_NAME, true))
                .andExpect(cookie().secure(RefreshCookies.COOKIE_NAME, true))
                .andExpect(cookie().path(RefreshCookies.COOKIE_NAME, RefreshCookies.COOKIE_PATH))
                .andReturn();

        JsonNode loginBody = json.readTree(login.getResponse().getContentAsString());
        String access1 = loginBody.path("data").path("accessToken").asText();
        jakarta.servlet.http.Cookie refreshCookie1 = login.getResponse().getCookie(RefreshCookies.COOKIE_NAME);
        assertThat(refreshCookie1).isNotNull();
        assertThat(refreshCookie1.getValue()).isNotBlank();

        // 3. Hit /auth/me with the Bearer — must return the same email
        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + access1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));

        // 4. Refresh with the cookie — new access token + rotated cookie
        MvcResult refresh = mockMvc.perform(post("/auth/refresh")
                        .cookie(refreshCookie1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(cookie().exists(RefreshCookies.COOKIE_NAME))
                .andReturn();
        JsonNode refreshBody = json.readTree(refresh.getResponse().getContentAsString());
        String access2 = refreshBody.path("data").path("accessToken").asText();
        jakarta.servlet.http.Cookie refreshCookie2 = refresh.getResponse().getCookie(RefreshCookies.COOKIE_NAME);
        assertThat(access2).isNotBlank();
        assertThat(refreshCookie2).isNotNull();
        assertThat(refreshCookie2.getValue()).isNotEqualTo(refreshCookie1.getValue()); // rotated

        // 5. Old refresh cookie replay → 401 (revoked on rotation)
        mockMvc.perform(post("/auth/refresh").cookie(refreshCookie1))
                .andExpect(status().isUnauthorized());

        // 6. /auth/me with the SECOND access token still works
        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + access2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));

        // 7. Logout with the current cookie
        mockMvc.perform(post("/auth/logout").cookie(refreshCookie2))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(RefreshCookies.COOKIE_NAME, 0));

        // 8. Post-logout refresh → 401
        mockMvc.perform(post("/auth/refresh").cookie(refreshCookie2))
                .andExpect(status().isUnauthorized());

        // 9. Second logout is a no-op (idempotent)
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent());
    }
}
