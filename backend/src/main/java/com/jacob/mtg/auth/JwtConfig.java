package com.jacob.mtg.auth;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * JWTs are signed with HMAC-SHA256 using one server-side secret.
 * The encoder signs tokens at login; the decoder (used by Spring Security on every request)
 * checks the signature, the expiry and the issuer.
 */
@Configuration
class JwtConfig {

    @Bean
    SecretKey jwtSigningKey(AuthProperties properties) {
        byte[] keyBytes = Base64.getDecoder().decode(properties.jwtSecret());
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.auth.jwt-secret must be at least 256 bits (32 bytes, Base64-encoded)");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey, AuthProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
        // default validators = expiry/not-before; plus: token must have been issued by us
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }
}
