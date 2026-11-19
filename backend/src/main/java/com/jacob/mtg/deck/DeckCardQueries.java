package com.jacob.mtg.deck;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.jacob.mtg.card.dto.CardDtos.Images;
import com.jacob.mtg.card.dto.CardDtos.Prices;

/**
 * Read side of decks (CQRS-lite): JPA writes the Deck aggregate, these SQL queries read deck cards
 * joined with the card catalogue in one round trip, which JPA could not do without card entities.
 */
@Repository
public class DeckCardQueries {

    /** One row per deck entry with everything the UI and the validator need. */
    public record DeckCardView(
        Board board,
        int quantity,
        UUID scryfallId,
        String setCode,
        String collectorNumber,
        String rarity,
        Images images,
        Prices prices,
        UUID oracleId,
        String name,
        String manaCost,
        BigDecimal cmc,
        String typeLine,
        String oracleText,
        List<String> colors,
        List<String> colorIdentity,
        List<String> keywords,
        String legality) {
    }

    public record DeckStats(long deckId, int cardCount, String coverImage, BigDecimal totalEur) {
    }

    public record ResolvedCard(UUID oracleId, UUID defaultScryfallId, String name) {
    }

    static final String DECK_CARDS = """
        SELECT dc.board, dc.quantity, p.scryfall_id, p.set_code, p.collector_number, p.rarity,
               p.image_uris ->> 'small' AS img_small, p.image_uris ->> 'normal' AS img_normal,
               p.image_uris ->> 'large' AS img_large, p.image_uris ->> 'art_crop' AS img_art,
               p.price_usd, p.price_usd_foil, p.price_usd_etched, p.price_eur, p.price_eur_foil, p.price_tix,
               o.oracle_id, o.name, o.mana_cost, o.cmc, o.type_line, o.oracle_text,
               o.colors, o.color_identity, o.keywords, o.legalities ->> :format AS legality
        FROM deck_cards dc
        JOIN card_printings p ON p.scryfall_id = dc.scryfall_id
        JOIN oracle_cards o ON o.oracle_id = p.oracle_id
        WHERE dc.deck_id = :deckId
        ORDER BY dc.board, o.cmc, o.name, p.set_code
        """;

    /** Card count (main + commander), a cover image (commander first, else most expensive-looking card), value. */
    static final String DECK_STATS = """
        SELECT dc.deck_id,
               COALESCE(sum(dc.quantity) FILTER (WHERE dc.board IN ('MAIN', 'COMMANDER')), 0) AS card_count,
               (array_agg(p.image_uris ->> 'art_crop'
                          ORDER BY (dc.board = 'COMMANDER') DESC, o.cmc DESC, o.name))[1] AS cover,
               sum(dc.quantity * p.price_eur) FILTER (WHERE dc.board <> 'MAYBEBOARD') AS total_eur
        FROM deck_cards dc
        JOIN card_printings p ON p.scryfall_id = dc.scryfall_id
        JOIN oracle_cards o ON o.oracle_id = p.oracle_id
        WHERE dc.deck_id IN (:deckIds)
        GROUP BY dc.deck_id
        """;

    /**
     * Exact name, case-insensitive. Also matches the front face of double-faced cards, because deck
     * sites often write "Delver of Secrets" for "Delver of Secrets // Insectile Aberration".
     */
    static final String RESOLVE_NAMES = """
        SELECT lower(o.name) AS full_name, lower(split_part(o.name, ' // ', 1)) AS front_name,
               o.oracle_id, o.default_scryfall_id, o.name
        FROM oracle_cards o
        WHERE o.default_scryfall_id IS NOT NULL
          AND (lower(o.name) IN (:names) OR lower(split_part(o.name, ' // ', 1)) IN (:names))
        """;

    static final String FIND_PRINTING = """
        SELECT scryfall_id FROM card_printings
        WHERE oracle_id = :oracleId AND set_code = :setCode AND (:number IS NULL OR collector_number = :number)
        ORDER BY collector_number
        LIMIT 1
        """;

    private final NamedParameterJdbcTemplate jdbc;

    public DeckCardQueries(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    public List<DeckCardView> findCards(long deckId, DeckFormat format) {
        return jdbc.query(DECK_CARDS,
            new MapSqlParameterSource("deckId", deckId).addValue("format", format.legalityKey()),
            (rs, i) -> new DeckCardView(
                Board.valueOf(rs.getString("board")),
                rs.getInt("quantity"),
                rs.getObject("scryfall_id", UUID.class),
                rs.getString("set_code"),
                rs.getString("collector_number"),
                rs.getString("rarity"),
                new Images(rs.getString("img_small"), rs.getString("img_normal"),
                    rs.getString("img_large"), rs.getString("img_art")),
                new Prices(rs.getBigDecimal("price_usd"), rs.getBigDecimal("price_usd_foil"),
                    rs.getBigDecimal("price_usd_etched"), rs.getBigDecimal("price_eur"),
                    rs.getBigDecimal("price_eur_foil"), rs.getBigDecimal("price_tix")),
                rs.getObject("oracle_id", UUID.class),
                rs.getString("name"),
                rs.getString("mana_cost"),
                rs.getBigDecimal("cmc"),
                rs.getString("type_line"),
                rs.getString("oracle_text"),
                strings(rs.getArray("colors")),
                strings(rs.getArray("color_identity")),
                strings(rs.getArray("keywords")),
                rs.getString("legality")));
    }

    public Map<Long, DeckStats> stats(Collection<Long> deckIds) {
        Map<Long, DeckStats> result = new HashMap<>();
        if (deckIds.isEmpty()) {
            return result;
        }
        jdbc.query(DECK_STATS, new MapSqlParameterSource("deckIds", deckIds), (ResultSet rs) -> {
            long id = rs.getLong("deck_id");
            result.put(id, new DeckStats(id, rs.getInt("card_count"), rs.getString("cover"), rs.getBigDecimal("total_eur")));
        });
        return result;
    }

    public boolean printingExists(UUID scryfallId) {
        Boolean exists = jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM card_printings WHERE scryfall_id = :id)",
            Map.of("id", scryfallId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    /** @return lower-case input name -> card. Exact full-name matches win over front-face matches. */
    public Map<String, ResolvedCard> resolveNames(Collection<String> names) {
        Map<String, ResolvedCard> result = new HashMap<>();
        if (names.isEmpty()) {
            return result;
        }
        List<String> lower = names.stream().map(n -> n.toLowerCase(Locale.ROOT)).distinct().toList();
        jdbc.query(RESOLVE_NAMES, new MapSqlParameterSource("names", lower), (ResultSet rs) -> {
            ResolvedCard card = new ResolvedCard(rs.getObject("oracle_id", UUID.class),
                rs.getObject("default_scryfall_id", UUID.class), rs.getString("name"));
            String fullName = rs.getString("full_name");
            String frontName = rs.getString("front_name");
            if (lower.contains(fullName)) {
                result.put(fullName, card);
            }
            if (lower.contains(frontName) && !result.containsKey(frontName)) {
                result.put(frontName, card);
            }
        });
        return result;
    }

    public Optional<UUID> findPrinting(UUID oracleId, String setCode, String collectorNumber) {
        MapSqlParameterSource params = new MapSqlParameterSource("oracleId", oracleId)
            .addValue("setCode", setCode)
            .addValue("number", collectorNumber, java.sql.Types.VARCHAR);
        return jdbc.query(FIND_PRINTING, params, (rs, i) -> rs.getObject("scryfall_id", UUID.class))
            .stream().findFirst();
    }

    private static List<String> strings(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        return Arrays.stream((Object[]) array.getArray()).map(String::valueOf).toList();
    }
}
