package com.jacob.mtg.card.importer;

import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jacob.mtg.card.importer.CardRows.ImportRow;
import com.jacob.mtg.card.scryfall.BulkDataInfo;
import com.jacob.mtg.card.scryfall.ScryfallCard;
import com.jacob.mtg.card.scryfall.ScryfallClient;
import com.jacob.mtg.card.scryfall.ScryfallJson;
import com.jacob.mtg.card.scryfall.ScryfallProperties;

import tools.jackson.databind.MappingIterator;

/**
 * Imports Scryfall's daily bulk file (~100k printings, ~500 MB of JSON) into PostgreSQL.
 *
 * The file is a single JSON array. Instead of loading it all (that would need gigabytes of RAM),
 * Jackson's MappingIterator reads it as a stream: one card object at a time, mapped, buffered into
 * batches of 1000 and written. Memory use stays flat no matter how big the file gets.
 */
@Service
public class CardImportService {

    private static final Logger log = LoggerFactory.getLogger(CardImportService.class);
    static final int BATCH_SIZE = 1000;

    public record ImportResult(int imported, int skipped) {
    }

    private final ScryfallClient scryfallClient;
    private final CardBatchWriter writer;
    private final ImportLogRepository importLog;
    private final ScryfallProperties properties;

    public CardImportService(ScryfallClient scryfallClient, CardBatchWriter writer,
                             ImportLogRepository importLog, ScryfallProperties properties) {
        this.scryfallClient = scryfallClient;
        this.writer = writer;
        this.importLog = importLog;
        this.properties = properties;
    }

    /**
     * Full import from Scryfall. Never throws: failures are logged and recorded in scryfall_imports.
     *
     * @param force import even if this exact file was already imported
     */
    public void importFromScryfall(boolean force) {
        long runId = -1;
        int processed = 0;
        try {
            BulkDataInfo info = scryfallClient.fetchBulkInfo();
            OffsetDateTime fileVersion = OffsetDateTime.parse(info.updatedAt());

            Optional<Instant> lastImported = importLog.lastSuccessfulBulkUpdate(properties.bulkType());
            if (!force && lastImported.isPresent() && !fileVersion.toInstant().isAfter(lastImported.get())) {
                log.info("Scryfall bulk file {} already imported, skipping", info.updatedAt());
                return;
            }

            runId = importLog.start(properties.bulkType(), fileVersion);
            log.info("Importing Scryfall {} ({} MB, generated {})",
                properties.bulkType(), info.size() == null ? "?" : info.size() / 1_000_000, info.updatedAt());
            long started = System.currentTimeMillis();

            try (InputStream in = scryfallClient.openBulkFile(info)) {
                ImportResult result = importFrom(in);
                processed = result.imported();
                importLog.finish(runId, processed);
                log.info("Scryfall import done: {} printings imported, {} skipped, in {} s",
                    result.imported(), result.skipped(), (System.currentTimeMillis() - started) / 1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            recordFailure(runId, processed, e);
        } catch (Exception e) {
            recordFailure(runId, processed, e);
        }
    }

    /** Streams a JSON array of Scryfall cards into the database. Public so tests can feed a small fixture file. */
    public ImportResult importFrom(InputStream json) {
        int imported = 0;
        int skipped = 0;
        List<ImportRow> batch = new ArrayList<>(BATCH_SIZE);

        try (MappingIterator<ScryfallCard> cards = ScryfallJson.MAPPER.readerFor(ScryfallCard.class).readValues(json)) {
            while (cards.hasNextValue()) {
                Optional<ImportRow> row = ScryfallCardMapper.map(cards.nextValue());
                if (row.isEmpty()) {
                    skipped++;
                    continue;
                }
                batch.add(row.get());
                if (batch.size() == BATCH_SIZE) {
                    int written = writer.write(batch);
                    imported += written;
                    skipped += batch.size() - written;
                    batch.clear();
                    if (imported / BATCH_SIZE % 20 == 0) {
                        log.info("... {} printings imported", imported);
                    }
                }
            }
            int written = writer.write(batch);
            imported += written;
            skipped += batch.size() - written;
        }

        int changed = writer.refreshDefaultPrintings();
        log.debug("Default printing updated for {} cards", changed);
        return new ImportResult(imported, skipped);
    }

    private void recordFailure(long runId, int processed, Exception e) {
        log.error("Scryfall import failed after {} printings", processed, e);
        if (runId > 0) {
            importLog.fail(runId, processed, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
