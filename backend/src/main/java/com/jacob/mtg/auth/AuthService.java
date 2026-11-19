package com.jacob.mtg.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jacob.mtg.auth.dto.AuthResponse;
import com.jacob.mtg.auth.dto.LoginRequest;
import com.jacob.mtg.auth.dto.RegisterRequest;
import com.jacob.mtg.common.exception.ApiException;
import com.jacob.mtg.legal.ConsentDocument;
import com.jacob.mtg.legal.ConsentLog;
import com.jacob.mtg.legal.ConsentLogRepository;
import com.jacob.mtg.legal.LegalProperties;
import com.jacob.mtg.user.User;
import com.jacob.mtg.user.UserRepository;
import com.jacob.mtg.user.UserResponse;

@Service
@Transactional
public class AuthService {

    /** BCrypt silently ignores everything after 72 bytes (and Spring Security rejects longer input). */
    private static final int BCRYPT_MAX_BYTES = 72;
    private static final String INVALID_CREDENTIALS = "Invalid email or password";
    private static final String INVALID_SESSION = "Session expired, please log in again";

    private final SecureRandom secureRandom = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ConsentLogRepository consentLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttempts;
    private final AuthProperties authProperties;
    private final LegalProperties legalProperties;

    /** Compared against when the email does not exist, so both cases take the same time (no user enumeration via timing). */
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       ConsentLogRepository consentLogRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       LoginAttemptService loginAttempts,
                       AuthProperties authProperties,
                       LegalProperties legalProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.consentLogRepository = consentLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
        this.authProperties = authProperties;
        this.legalProperties = legalProperties;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public AuthResult register(RegisterRequest request, String userAgent) {
        if (exceedsBcryptLimit(request.password())) {
            throw ApiException.badRequest("Password is too long");
        }
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }

        User user = userRepository.save(
            new User(email, passwordEncoder.encode(request.password()), request.displayName().trim()));

        Instant now = Instant.now();
        consentLogRepository.saveAll(List.of(
            new ConsentLog(user, ConsentDocument.TOS, legalProperties.tosVersion(), now),
            new ConsentLog(user, ConsentDocument.PRIVACY, legalProperties.privacyVersion(), now)));

        return issueTokens(user, userAgent);
    }

    public AuthResult login(LoginRequest request, String clientIp, String userAgent) {
        String email = normalizeEmail(request.email());
        String attemptKey = email + "|" + clientIp;
        loginAttempts.checkNotLocked(attemptKey);

        Optional<User> user = userRepository.findByEmailAndDeletedAtIsNull(email);
        boolean valid = !exceedsBcryptLimit(request.password())
            && passwordEncoder.matches(request.password(), user.map(User::getPasswordHash).orElse(dummyPasswordHash))
            && user.isPresent();

        if (!valid) {
            loginAttempts.recordFailure(attemptKey);
            // Same message for "no such email" and "wrong password": never reveal which accounts exist.
            throw ApiException.unauthorized(INVALID_CREDENTIALS);
        }
        loginAttempts.recordSuccess(attemptKey);
        return issueTokens(user.get(), userAgent);
    }

    /**
     * Refresh token rotation: every refresh token works exactly once. Using it returns a new pair
     * and revokes the old one. If an already-used token shows up again, someone copied it, so every
     * session of that user is revoked (the legitimate user just logs in again).
     */
    @Transactional(noRollbackFor = ApiException.class) // the "revoke everything" must be saved even though we throw
    public AuthResult refresh(String rawRefreshToken, String userAgent) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw ApiException.unauthorized(INVALID_SESSION);
        }
        RefreshToken token = refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken))
            .orElseThrow(() -> ApiException.unauthorized(INVALID_SESSION));

        Instant now = Instant.now();
        if (token.isRevoked()) {
            refreshTokenRepository.revokeAllForUser(token.getUser().getId(), now);
            throw ApiException.unauthorized(INVALID_SESSION);
        }
        if (token.isExpired(now) || token.getUser().isDeleted()) {
            throw ApiException.unauthorized(INVALID_SESSION);
        }

        token.revoke(now);
        return issueTokens(token.getUser(), userAgent);
    }

    /** Idempotent: logging out with a missing/unknown/already revoked token is not an error. */
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken))
            .ifPresent(token -> token.revoke(Instant.now()));
    }

    private AuthResult issueTokens(User user, String userAgent) {
        String rawRefreshToken = generateRefreshToken();
        refreshTokenRepository.save(new RefreshToken(
            user,
            sha256(rawRefreshToken),
            Instant.now().plus(authProperties.refreshTokenTtl()),
            truncate(userAgent, 255)));

        AuthResponse body = AuthResponse.bearer(
            jwtService.createAccessToken(user),
            authProperties.accessTokenTtl().toSeconds(),
            UserResponse.from(user));
        return new AuthResult(body, rawRefreshToken);
    }

    /** 256 bits from a cryptographically secure generator, URL-safe so it fits in a cookie. */
    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e); // every JVM ships SHA-256
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean exceedsBcryptLimit(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
