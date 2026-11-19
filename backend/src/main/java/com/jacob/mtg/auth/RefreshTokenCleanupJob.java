package com.jacob.mtg.auth;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** GDPR data minimisation + keeps the table small: expired sessions are deleted after a short grace period. */
@Component
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);
    private static final Duration GRACE_PERIOD = Duration.ofDays(7);

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanupJob(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(cron = "0 30 3 * * *") // every day at 03:30
    @Transactional
    public void deleteExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredBefore(Instant.now().minus(GRACE_PERIOD));
        log.info("Deleted {} expired refresh tokens", deleted);
    }
}
