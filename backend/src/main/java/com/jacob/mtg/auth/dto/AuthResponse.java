package com.jacob.mtg.auth.dto;

import com.jacob.mtg.user.UserResponse;

/**
 * Body returned by register / login / refresh. The refresh token is NOT here:
 * it travels only in an HttpOnly cookie, so JavaScript (and XSS) can never read it.
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    public static AuthResponse bearer(String accessToken, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds, user);
    }
}
