package com.jacob.mtg.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Type-safe binding of app.auth.* from application.yml. */
@ConfigurationProperties("app.auth")
public record AuthProperties(
    String jwtSecret,
    String issuer,
    Duration accessTokenTtl,
    Duration refreshTokenTtl,
    boolean cookieSecure,
    String cookieSameSite,
    int maxLoginAttempts,
    Duration loginLockDuration) {
}
