package com.jacob.mtg.card.importer;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Audit trail of imports (table scryfall_imports): when, which file, how many cards, errors. */
@Repository
public class ImportLogRepository {

    public record ImportRun(long id, String bulkType, OffsetDateTime bulkUpdatedAt, OffsetDateTime startedAt,
                            OffsetDateTime finishedAt, String status, int cardsProcessed, String errorMessage) {
    }

    private final JdbcTemplate jdbc;

    public ImportLogRepository(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public long start(String bulkType, OffsetDateTime bulkUpdatedAt) {
        Long id = jdbc.queryForObject("""
            INSERT INTO scryfall_imports (bulk_type, bulk_updated_at, status)
            VALUES (?, ?, 'RUNNING') RETURNING id
            """, Long.class, bulkType, bulkUpdatedAt);
        if (id == null) {
            throw new IllegalStateException("Could not create import log row");
        }
        return id;
    }

    public void finish(long id, int cardsProcessed) {
        jdbc.update("""
            UPDATE scryfall_imports SET status = 'SUCCESS', finished_at = ?, cards_processed = ? WHERE id = ?
            """, OffsetDateTime.now(), cardsProcessed, id);
    }

    public void fail(long id, int cardsProcessed, String error) {
        jdbc.update("""
            UPDATE scryfall_imports SET status = 'FAILED', finished_at = ?, cards_processed = ?, error_message = ?
            WHERE id = ?
            """, OffsetDateTime.now(), cardsProcessed, truncate(error), id);
    }

    /** Version of the newest file we imported successfully (to skip re-importing the same file). */
    public Optional<Instant> lastSuccessfulBulkUpdate(String bulkType) {
        List<OffsetDateTime> result = jdbc.query("""
            SELECT bulk_updated_at FROM scryfall_imports
            WHERE bulk_type = ? AND status = 'SUCCESS' AND bulk_updated_at IS NOT NULL
            ORDER BY bulk_updated_at DESC LIMIT 1
            """, (rs, i) -> rs.getObject(1, OffsetDateTime.class), bulkType);
        return result.stream().findFirst().map(OffsetDateTime::toInstant);
    }

    public List<ImportRun> latest(int limit) {
        return jdbc.query("""
            SELECT id, bulk_type, bulk_updated_at, started_at, finished_at, status, cards_processed, error_message
            FROM scryfall_imports ORDER BY started_at DESC LIMIT ?
            """, (rs, i) -> new ImportRun(
                rs.getLong("id"),
                rs.getString("bulk_type"),
                rs.getObject("bulk_updated_at", OffsetDateTime.class),
                rs.getObject("started_at", OffsetDateTime.class),
                rs.getObject("finished_at", OffsetDateTime.class),
                rs.getString("status"),
                rs.getInt("cards_processed"),
                rs.getString("error_message")),
            limit);
    }

    /** A crash or restart mid-import leaves RUNNING rows behind; mark them so the log stays truthful. */
    public int markAbandonedRuns() {
        return jdbc.update("""
            UPDATE scryfall_imports SET status = 'FAILED', finished_at = now(),
                   error_message = 'Interrupted (application stopped during import)'
            WHERE status = 'RUNNING'
            """);
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }
}
