package com.jacob.mtg.auth;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jacob.mtg.auth.dto.AuthResponse;
import com.jacob.mtg.auth.dto.LoginRequest;
import com.jacob.mtg.auth.dto.RegisterRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Two tokens:
 *  - access token (JWT, 15 min) in the JSON body: the frontend keeps it in memory and sends
 *    it as "Authorization: Bearer ..." on every API call;
 *  - refresh token (random, 14 days) in an HttpOnly cookie scoped to /api/auth: used only
 *    to get a new access token when the old one expires.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String REFRESH_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final AuthProperties properties;

    public AuthController(AuthService authService, AuthProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest http) {
        AuthResult result = authService.register(request, http.getHeader(HttpHeaders.USER_AGENT));
        return withRefreshCookie(ResponseEntity.status(HttpStatus.CREATED), result);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest http) {
        AuthResult result = authService.login(request, http.getRemoteAddr(), http.getHeader(HttpHeaders.USER_AGENT));
        return withRefreshCookie(ResponseEntity.ok(), result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
                                                HttpServletRequest http) {
        AuthResult result = authService.refresh(refreshToken, http.getHeader(HttpHeaders.USER_AGENT));
        return withRefreshCookie(ResponseEntity.ok(), result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString()) // tells the browser to delete it
            .build();
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(ResponseEntity.BodyBuilder response, AuthResult result) {
        return response
            .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken(), properties.refreshTokenTtl()).toString())
            .body(result.body());
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
            .httpOnly(true)                       // invisible to JavaScript -> safe from XSS theft
            .secure(properties.cookieSecure())    // HTTPS only in production
            .sameSite(properties.cookieSameSite()) // not sent by cross-site requests -> CSRF protection
            .path(COOKIE_PATH)                    // only sent to the auth endpoints, not to every API call
            .maxAge(maxAge)
            .build();
    }
}
