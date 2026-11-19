package com.jacob.mtg.auth;

import com.jacob.mtg.auth.dto.AuthResponse;

/** Internal: what the service hands to the controller (body + raw refresh token for the cookie). */
public record AuthResult(AuthResponse body, String refreshToken) {
}
