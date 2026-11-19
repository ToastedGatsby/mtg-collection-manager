package com.jacob.mtg.auth;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.jacob.mtg.user.User;

/** Creates short-lived access tokens. Validation is done by Spring Security (see JwtConfig + SecurityConfig). */
@Service
public class JwtService {

    static final String ROLES_CLAIM = "roles";

    private final JwtEncoder jwtEncoder;
    private final AuthProperties properties;

    public JwtService(JwtEncoder jwtEncoder, AuthProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        // Keep the payload minimal: a JWT is only signed, not encrypted - anyone can read it (jwt.io).
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .issuedAt(now)
            .expiresAt(now.plus(properties.accessTokenTtl()))
            .subject(user.getId().toString())
            .claim(ROLES_CLAIM, List.of(user.getRole().name()))
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
