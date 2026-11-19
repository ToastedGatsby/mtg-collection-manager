package com.jacob.mtg.card.importer;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jacob.mtg.card.scryfall.ScryfallProperties;

/**
 * Decides WHEN to import. Three entry points, one guard so only one import ever runs at a time:
 *  - on startup, if the card table is empty (first run on a new machine / new database);
 *  - every night (cron in application.yml);
 *  - manually by an admin (AdminCardImportController).
 * Imports run on a background (virtual) thread, so the app starts and serves requests meanwhile.
 */
@Component
public class ScryfallImportTrigger {

    private static final Logger log = LoggerFactory.getLogger(ScryfallImportTrigger.class);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final CardImportService importService;
    private final CardBatchWriter writer;
    private final ImportLogRepository importLog;
    private final ScryfallProperties properties;

    public ScryfallImportTrigger(CardImportService importService, CardBatchWriter writer,
                                 ImportLogRepository importLog, ScryfallProperties properties) {
        this.importService = importService;
        this.writer = writer;
        this.importLog = importLog;
        this.properties = properties;
    }

    /** @return false if an import is already running */
    public boolean startAsync(boolean force) {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        Thread.ofVirtual().name("scryfall-import").start(() -> {
            try {
                importService.importFromScryfall(force);
            } finally {
                running.set(false);
            }
        });
        return true;
    }

    public boolean isRunning() {
        return running.get();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        int abandoned = importLog.markAbandonedRuns();
        if (abandoned > 0) {
            log.warn("Marked {} interrupted import run(s) as FAILED", abandoned);
        }
        if (properties.importOnStartup() && writer.countOracleCards() == 0) {
            log.info("Card table is empty: starting the first Scryfall import in the background (takes a few minutes)");
            startAsync(true);
        }
    }

    @Scheduled(cron = "${app.scryfall.import-cron}")
    public void nightly() {
        startAsync(false);
    }
}
