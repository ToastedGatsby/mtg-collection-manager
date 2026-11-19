package com.jacob.mtg.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jacob.mtg.common.exception.ApiException;

/**
 * Brute-force protection: after N failed logins for the same email from the same IP,
 * that pair is locked for a while. Keyed by email+IP (not email alone) so an attacker
 * cannot lock a real user out of their account from another machine.
 *
 * In-memory is enough for a single instance. With several instances this would move to Redis.
 */
@Component
public class LoginAttemptService {

    private record Attempts(int failures, Instant windowStart, Instant lockedUntil) {
    }

    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration lockDuration;

    public LoginAttemptService(AuthProperties properties) {
        this.maxAttempts = properties.maxLoginAttempts();
        this.lockDuration = properties.loginLockDuration();
    }

    public void checkNotLocked(String key) {
        Attempts current = attempts.get(key);
        if (current == null || current.lockedUntil() == null) {
            return;
        }
        if (current.lockedUntil().isAfter(Instant.now())) {
            throw ApiException.tooManyRequests("Too many failed login attempts. Try again later.");
        }
        attempts.remove(key); // lock expired -> start fresh
    }

    public void recordFailure(String key) {
        Instant now = Instant.now();
        attempts.compute(key, (k, current) -> {
            Attempts base = (current == null || current.windowStart().plus(lockDuration).isBefore(now))
                ? new Attempts(0, now, null)
                : current;
            int failures = base.failures() + 1;
            Instant lockedUntil = failures >= maxAttempts ? now.plus(lockDuration) : null;
            return new Attempts(failures, base.windowStart(), lockedUntil);
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    /** Keeps the map from growing forever. */
    @Scheduled(fixedRate = 3_600_000)
    public void evictStaleEntries() {
        Instant cutoff = Instant.now().minus(lockDuration.multipliedBy(2));
        attempts.entrySet().removeIf(e -> e.getValue().windowStart().isBefore(cutoff));
    }
}
