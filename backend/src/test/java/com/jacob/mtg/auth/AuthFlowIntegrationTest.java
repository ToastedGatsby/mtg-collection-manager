package com.jacob.mtg.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.jacob.mtg.legal.ConsentLogRepository;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/**
 * End-to-end tests of the auth API through the real Spring Security filter chain and real PostgreSQL
 * (docker compose locally, a service container in CI). @Transactional rolls every test back.
 */
@SpringBootTest
@Transactional
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "Correct-Horse-Battery-42";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ConsentLogRepository consentLogRepository;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void fullFlow_register_me_login_refresh_logout() throws Exception {
        String email = uniqueEmail();

        // register -> 201, access token in body, refresh token in HttpOnly cookie, consent stored
        MvcResult registered = register(email, PASSWORD);
        String accessToken = accessToken(registered);
        Cookie refreshCookie = refreshCookie(registered);
        assertThat(refreshCookie.isHttpOnly()).isTrue();
        Long userId = ((Number) JsonPath.read(registered.getResponse().getContentAsString(), "$.user.id")).longValue();
        assertThat(consentLogRepository.findByUserId(userId)).hasSize(2);

        // the access token opens protected endpoints
        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.role").value("USER"));

        // login with the same credentials (email is case-insensitive)
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email.toUpperCase(), PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // refresh -> new pair, and the cookie value changes (rotation)
        MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
            .andExpect(status().isOk())
            .andReturn();
        Cookie rotatedCookie = refreshCookie(refreshed);
        assertThat(rotatedCookie.getValue()).isNotEqualTo(refreshCookie.getValue());

        // logout -> 204 and the cookie is cleared
        MvcResult loggedOut = mvc.perform(post("/api/auth/logout").cookie(rotatedCookie))
            .andExpect(status().isNoContent())
            .andReturn();
        assertThat(refreshCookie(loggedOut).getMaxAge()).isZero();

        // the logged-out token can no longer be used
        mvc.perform(post("/api/auth/refresh").cookie(rotatedCookie))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void reusingAnOldRefreshToken_revokesAllSessions() throws Exception {
        Cookie original = refreshCookie(register(uniqueEmail(), PASSWORD));

        Cookie rotated = refreshCookie(mvc.perform(post("/api/auth/refresh").cookie(original))
            .andExpect(status().isOk())
            .andReturn());

        // an attacker replays the old token -> rejected...
        mvc.perform(post("/api/auth/refresh").cookie(original))
            .andExpect(status().isUnauthorized());

        // ...and the legitimate (newer) session is killed too
        mvc.perform(post("/api/auth/refresh").cookie(rotated))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordAndUnknownEmail_returnTheSameGenericError() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, "wrong-password-123")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Invalid email or password"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(uniqueEmail(), "wrong-password-123")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void tooManyFailedLogins_lockTheAccountFromThatIp() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson(email, "wrong-password-123")))
                .andExpect(status().isUnauthorized());
        }
        // even the correct password is refused while locked
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, PASSWORD)))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void registerWithoutAcceptingTerms_isRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(uniqueEmail(), PASSWORD, false)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.acceptTerms").exists());
    }

    @Test
    void registerWithShortPassword_isRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(uniqueEmail(), "short", true)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void registerTwiceWithSameEmail_returnsConflict() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(email, PASSWORD, true)))
            .andExpect(status().isConflict());
    }

    @Test
    void protectedEndpoint_withoutOrWithForgedToken_returns401() throws Exception {
        mvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.real.jwt"))
            .andExpect(status().isUnauthorized());
    }

    // ---------- helpers ----------

    private MvcResult register(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(email, password, true)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.email").value(email))
            .andReturn();
    }

    private static String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }

    private static String registerJson(String email, String password, boolean acceptTerms) {
        return """
            {"displayName": "Tester", "email": "%s", "password": "%s", "acceptTerms": %s}
            """.formatted(email, password, acceptTerms);
    }

    private static String loginJson(String email, String password) {
        return """
            {"email": "%s", "password": "%s"}
            """.formatted(email, password);
    }

    private static String accessToken(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static Cookie refreshCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie(AuthController.REFRESH_COOKIE);
        assertThat(cookie).as("refresh_token cookie").isNotNull();
        return cookie;
    }
}
