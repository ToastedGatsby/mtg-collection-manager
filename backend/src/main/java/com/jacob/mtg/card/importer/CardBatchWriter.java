package com.jacob.mtg.card.importer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.jacob.mtg.card.importer.CardRows.ImportRow;
import com.jacob.mtg.card.importer.CardRows.OracleRow;
import com.jacob.mtg.card.importer.CardRows.PrintingRow;

/**
 * Writes cards with plain JDBC batches instead of JPA: 100k upserts through Hibernate would mean
 * 100k SELECT + INSERT/UPDATE pairs; one "INSERT ... ON CONFLICT DO UPDATE" batch per 1000 cards
 * does the same work in ~100 round trips, and re-running the import simply updates existing rows.
 */
@Component
public class CardBatchWriter {

    static final String UPSERT_ORACLE = """
        INSERT INTO oracle_cards (oracle_id, name, mana_cost, cmc, type_line, oracle_text,
                                  colors, color_identity, keywords, legalities, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?::varchar[], ?::varchar[], ?::text[], ?::jsonb, now())
        ON CONFLICT (oracle_id) DO UPDATE SET
            name = EXCLUDED.name,
            mana_cost = EXCLUDED.mana_cost,
            cmc = EXCLUDED.cmc,
            type_line = EXCLUDED.type_line,
            oracle_text = EXCLUDED.oracle_text,
            colors = EXCLUDED.colors,
            color_identity = EXCLUDED.color_identity,
            keywords = EXCLUDED.keywords,
            legalities = EXCLUDED.legalities,
            updated_at = now()
        """;

    static final String UPSERT_PRINTING = """
        INSERT INTO card_printings (scryfall_id, oracle_id, set_code, set_name, collector_number, rarity, lang,
                                    finishes, image_uris, tcgplayer_id, cardmarket_id, released_at, promo,
                                    border_color, purchase_uris, price_usd, price_usd_foil, price_usd_etched,
                                    price_eur, price_eur_foil, price_tix, prices_updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?::varchar[], ?::jsonb, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, now())
        ON CONFLICT (scryfall_id) DO UPDATE SET
            oracle_id = EXCLUDED.oracle_id,
            set_code = EXCLUDED.set_code,
            set_name = EXCLUDED.set_name,
            collector_number = EXCLUDED.collector_number,
            rarity = EXCLUDED.rarity,
            lang = EXCLUDED.lang,
            finishes = EXCLUDED.finishes,
            image_uris = EXCLUDED.image_uris,
            tcgplayer_id = EXCLUDED.tcgplayer_id,
            cardmarket_id = EXCLUDED.cardmarket_id,
            released_at = EXCLUDED.released_at,
            promo = EXCLUDED.promo,
            border_color = EXCLUDED.border_color,
            purchase_uris = EXCLUDED.purchase_uris,
            price_usd = EXCLUDED.price_usd,
            price_usd_foil = EXCLUDED.price_usd_foil,
            price_usd_etched = EXCLUDED.price_usd_etched,
            price_eur = EXCLUDED.price_eur,
            price_eur_foil = EXCLUDED.price_eur_foil,
            price_tix = EXCLUDED.price_tix,
            prices_updated_at = now()
        """;

    /**
     * Picks the printing shown in search results: has an image, English, not a promo,
     * not borderless, then the newest. DISTINCT ON keeps the first row per card (PostgreSQL feature).
     */
    static final String REFRESH_DEFAULT_PRINTINGS = """
        UPDATE oracle_cards o
        SET default_scryfall_id = d.scryfall_id
        FROM (
            SELECT DISTINCT ON (oracle_id) oracle_id, scryfall_id
            FROM card_printings
            ORDER BY oracle_id,
                     (image_uris IS NULL),
                     (lang <> 'en'),
                     promo,
                     (border_color IS NOT DISTINCT FROM 'borderless'),
                     released_at DESC NULLS LAST,
                     scryfall_id
        ) d
        WHERE o.oracle_id = d.oracle_id
          AND o.default_scryfall_id IS DISTINCT FROM d.scryfall_id
        """;

    private static final Logger log = LoggerFactory.getLogger(CardBatchWriter.class);

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public CardBatchWriter(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Writes a batch in one transaction. If the batch fails (one malformed card among 1000), it is
     * retried card by card so only the bad card is skipped instead of the whole import failing.
     *
     * @return number of rows written
     */
    public int write(List<ImportRow> rows) {
        if (rows.isEmpty()) {
            return 0;
        }
        try {
            writeBatch(rows);
            return rows.size();
        } catch (DataAccessException batchFailure) {
            log.warn("Batch of {} cards failed ({}), retrying one by one", rows.size(), batchFailure.getMostSpecificCause().getMessage());
            int written = 0;
            for (ImportRow row : rows) {
                try {
                    writeBatch(List.of(row));
                    written++;
                } catch (DataAccessException e) {
                    log.warn("Skipping card {} ({}): {}", row.oracle().name(), row.printing().scryfallId(),
                        e.getMostSpecificCause().getMessage());
                }
            }
            return written;
        }
    }

    private void writeBatch(List<ImportRow> rows) {
        // several printings of the same card in one batch -> upsert the oracle row only once
        Map<Object, OracleRow> oracles = new LinkedHashMap<>();
        List<PrintingRow> printings = new ArrayList<>(rows.size());
        for (ImportRow row : rows) {
            oracles.put(row.oracle().oracleId(), row.oracle());
            printings.add(row.printing());
        }

        transaction.executeWithoutResult(status -> {
            // oracle rows first: card_printings.oracle_id is a foreign key to them
            jdbc.batchUpdate(UPSERT_ORACLE, oracles.values(), oracles.size(), CardBatchWriter::bindOracle);
            jdbc.batchUpdate(UPSERT_PRINTING, printings, printings.size(), CardBatchWriter::bindPrinting);
        });
    }

    /** @return number of cards whose default printing changed */
    public int refreshDefaultPrintings() {
        Integer updated = transaction.execute(status -> jdbc.update(REFRESH_DEFAULT_PRINTINGS));
        return updated == null ? 0 : updated;
    }

    public long countOracleCards() {
        Long count = jdbc.queryForObject("SELECT count(*) FROM oracle_cards", Long.class);
        return count == null ? 0 : count;
    }

    private static void bindOracle(PreparedStatement ps, OracleRow o) throws SQLException {
        Connection con = ps.getConnection();
        ps.setObject(1, o.oracleId());
        ps.setString(2, o.name());
        ps.setString(3, o.manaCost());
        ps.setBigDecimal(4, o.cmc());
        ps.setString(5, o.typeLine());
        ps.setString(6, o.oracleText());
        ps.setArray(7, textArray(con, o.colors()));
        ps.setArray(8, textArray(con, o.colorIdentity()));
        ps.setArray(9, textArray(con, o.keywords()));
        ps.setString(10, o.legalitiesJson());
    }

    private static void bindPrinting(PreparedStatement ps, PrintingRow p) throws SQLException {
        ps.setObject(1, p.scryfallId());
        ps.setObject(2, p.oracleId());
        ps.setString(3, p.setCode());
        ps.setString(4, p.setName());
        ps.setString(5, p.collectorNumber());
        ps.setString(6, p.rarity());
        ps.setString(7, p.lang());
        ps.setArray(8, textArray(ps.getConnection(), p.finishes()));
        ps.setString(9, p.imageUrisJson());
        setNullableInt(ps, 10, p.tcgplayerId());
        setNullableInt(ps, 11, p.cardmarketId());
        ps.setObject(12, p.releasedAt(), Types.DATE);
        ps.setBoolean(13, p.promo());
        ps.setString(14, p.borderColor());
        ps.setString(15, p.purchaseUrisJson());
        setNullableDecimal(ps, 16, p.priceUsd());
        setNullableDecimal(ps, 17, p.priceUsdFoil());
        setNullableDecimal(ps, 18, p.priceUsdEtched());
        setNullableDecimal(ps, 19, p.priceEur());
        setNullableDecimal(ps, 20, p.priceEurFoil());
        setNullableDecimal(ps, 21, p.priceTix());
    }

    private static java.sql.Array textArray(Connection con, Collection<String> values) throws SQLException {
        return con.createArrayOf("text", values.toArray(String[]::new));
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private static void setNullableDecimal(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.NUMERIC);
        } else {
            ps.setBigDecimal(index, value);
        }
    }
}
