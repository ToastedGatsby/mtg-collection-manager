package com.jacob.mtg.card;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.jacob.mtg.card.dto.CardDtos.CardDetail;
import com.jacob.mtg.card.dto.CardDtos.CardSummary;
import com.jacob.mtg.card.dto.CardDtos.Images;
import com.jacob.mtg.card.dto.CardDtos.Prices;
import com.jacob.mtg.card.dto.CardDtos.Printing;
import com.jacob.mtg.card.scryfall.ScryfallJson;
import com.jacob.mtg.common.web.PageResponse;

/**
 * Card search with composable filters (the idea behind the Specification pattern): every filter the
 * user sets adds one SQL condition, all joined with AND. Native SQL instead of JPA Criteria because
 * the useful operators are PostgreSQL-specific: array containment (@> / <@) for colours, JSONB (->>)
 * for format legality, trigram-indexed ILIKE for names.
 *
 * Safety: user input only ever reaches the database as bound parameters (:name), never concatenated
 * into SQL. The only dynamic SQL pieces (format names, sort columns) come from fixed whitelists.
 */
@Repository
public class CardSearchRepository {

    static final String SELECT_SUMMARY = """
        SELECT o.oracle_id, o.name, o.mana_cost, o.cmc, o.type_line, o.colors, o.color_identity,
               d.scryfall_id AS default_id, d.set_code, d.rarity,
               d.image_uris ->> 'small' AS img_small, d.image_uris ->> 'normal' AS img_normal,
               d.image_uris ->> 'large' AS img_large, d.image_uris ->> 'art_crop' AS img_art,
               d.price_usd, d.price_usd_foil, d.price_usd_etched, d.price_eur, d.price_eur_foil, d.price_tix
        FROM oracle_cards o
        LEFT JOIN card_printings d ON d.scryfall_id = o.default_scryfall_id
        """;
    static final String COUNT = "SELECT count(*) FROM oracle_cards o LEFT JOIN card_printings d ON d.scryfall_id = o.default_scryfall_id";

    static final String NAME = "o.name ILIKE :q";
    static final String TEXT = "o.oracle_text ILIKE :text";
    static final String TYPE = "o.type_line ILIKE :type";
    static final String COLORS_INCLUDING = "o.colors @> CAST(:colors AS varchar[])";
    static final String COLORS_EXACT = "o.colors @> CAST(:colors AS varchar[]) AND o.colors <@ CAST(:colors AS varchar[])";
    static final String COLORLESS = "cardinality(o.colors) = 0";
    static final String COLOR_IDENTITY = "o.color_identity <@ CAST(:colors AS varchar[])";
    static final String CMC_MIN = "o.cmc >= :cmcMin";
    static final String CMC_MAX = "o.cmc <= :cmcMax";
    static final String RARITY = "EXISTS (SELECT 1 FROM card_printings p WHERE p.oracle_id = o.oracle_id AND p.rarity = :rarity)";
    static final String SET = "EXISTS (SELECT 1 FROM card_printings p WHERE p.oracle_id = o.oracle_id AND p.set_code = :set)";
    static final String FORMAT = "o.legalities ->> :format IN ('legal', 'restricted')";

    static final String SELECT_DETAIL = """
        SELECT o.oracle_id, o.name, o.mana_cost, o.cmc, o.type_line, o.oracle_text, o.colors, o.color_identity,
               o.keywords, o.legalities::text AS legalities, d.scryfall_id AS default_id,
               d.image_uris ->> 'small' AS img_small, d.image_uris ->> 'normal' AS img_normal,
               d.image_uris ->> 'large' AS img_large, d.image_uris ->> 'art_crop' AS img_art
        FROM oracle_cards o
        LEFT JOIN card_printings d ON d.scryfall_id = o.default_scryfall_id
        WHERE o.oracle_id = :id
        """;

    static final String SELECT_PRINTINGS = """
        SELECT scryfall_id, set_code, set_name, collector_number, rarity, lang, released_at, finishes, promo,
               image_uris ->> 'small' AS img_small, image_uris ->> 'normal' AS img_normal,
               image_uris ->> 'large' AS img_large, image_uris ->> 'art_crop' AS img_art,
               price_usd, price_usd_foil, price_usd_etched, price_eur, price_eur_foil, price_tix,
               purchase_uris ->> 'tcgplayer' AS tcgplayer_url, purchase_uris ->> 'cardmarket' AS cardmarket_url
        FROM card_printings
        WHERE oracle_id = :id
        ORDER BY released_at DESC NULLS LAST, set_code, collector_number
        """;

    static final Set<String> FORMATS = Set.of(
        "standard", "pioneer", "modern", "legacy", "vintage", "pauper", "commander", "oathbreaker",
        "brawl", "standardbrawl", "historic", "timeless", "alchemy", "explorer", "penny", "duel",
        "premodern", "oldschool", "predh", "paupercommander", "gladiator", "future");
    static final Set<String> RARITIES = Set.of("common", "uncommon", "rare", "mythic", "special", "bonus");
    static final Map<String, String> SORTS = Map.of(
        "name", "o.name ASC",
        "cmc", "o.cmc ASC, o.name ASC",
        "price", "d.price_eur DESC NULLS LAST, o.name ASC");
    private static final Pattern SET_CODE = Pattern.compile("[a-z0-9]{2,10}");
    private static final String WUBRG = "WUBRG";

    static final int DEFAULT_PAGE_SIZE = 40;
    static final int MAX_PAGE_SIZE = 100;

    private final NamedParameterJdbcTemplate jdbc;

    public CardSearchRepository(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    public PageResponse<CardSummary> search(CardSearchRequest request) {
        List<String> conditions = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (notBlank(request.q())) {
            conditions.add(NAME);
            params.addValue("q", contains(request.q()));
        }
        if (notBlank(request.text())) {
            conditions.add(TEXT);
            params.addValue("text", contains(request.text()));
        }
        if (notBlank(request.type())) {
            conditions.add(TYPE);
            params.addValue("type", contains(request.type()));
        }
        addColorFilter(request, conditions, params);
        if (request.cmcMin() != null) {
            conditions.add(CMC_MIN);
            params.addValue("cmcMin", request.cmcMin());
        }
        if (request.cmcMax() != null) {
            conditions.add(CMC_MAX);
            params.addValue("cmcMax", request.cmcMax());
        }
        String rarity = lower(request.rarity());
        if (rarity != null && RARITIES.contains(rarity)) {
            conditions.add(RARITY);
            params.addValue("rarity", rarity);
        }
        String set = lower(request.set());
        if (set != null && SET_CODE.matcher(set).matches()) {
            conditions.add(SET);
            params.addValue("set", set);
        }
        String format = lower(request.format());
        if (format != null && FORMATS.contains(format)) {
            conditions.add(FORMAT);
            params.addValue("format", format);
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        int size = request.size() == null ? DEFAULT_PAGE_SIZE : Math.clamp(request.size(), 1, MAX_PAGE_SIZE);
        int page = request.page() == null ? 0 : Math.clamp(request.page(), 0, 10_000);
        String orderBy = SORTS.getOrDefault(lower(request.sort()) == null ? "name" : lower(request.sort()), SORTS.get("name"));

        Long total = jdbc.queryForObject(COUNT + where, params, Long.class);
        params.addValue("limit", size).addValue("offset", (long) page * size);
        List<CardSummary> content = jdbc.query(
            SELECT_SUMMARY + where + " ORDER BY " + orderBy + ", o.oracle_id LIMIT :limit OFFSET :offset",
            params, (rs, i) -> mapSummary(rs));

        return PageResponse.of(content, page, size, total == null ? 0 : total);
    }

    public Optional<CardDetail> findDetail(UUID oracleId) {
        List<CardDetail> rows = jdbc.query(SELECT_DETAIL, Map.of("id", oracleId), (rs, i) -> new CardDetail(
            rs.getObject("oracle_id", UUID.class),
            rs.getString("name"),
            rs.getString("mana_cost"),
            rs.getBigDecimal("cmc"),
            rs.getString("type_line"),
            rs.getString("oracle_text"),
            strings(rs.getArray("colors")),
            strings(rs.getArray("color_identity")),
            strings(rs.getArray("keywords")),
            parseLegalities(rs.getString("legalities")),
            rs.getObject("default_id", UUID.class),
            images(rs)));
        return rows.stream().findFirst();
    }

    public List<Printing> findPrintings(UUID oracleId) {
        return jdbc.query(SELECT_PRINTINGS, Map.of("id", oracleId), (rs, i) -> new Printing(
            rs.getObject("scryfall_id", UUID.class),
            rs.getString("set_code"),
            rs.getString("set_name"),
            rs.getString("collector_number"),
            rs.getString("rarity"),
            rs.getString("lang"),
            rs.getObject("released_at", LocalDate.class),
            strings(rs.getArray("finishes")),
            rs.getBoolean("promo"),
            images(rs),
            prices(rs),
            rs.getString("tcgplayer_url"),
            rs.getString("cardmarket_url")));
    }

    /**
     * MTGO-style colour modes:
     *  including - has at least these colours (R -> mono-red and every multicolour card with red);
     *  exact     - exactly these colours;
     *  identity  - colour identity fits inside these colours (what a Commander deck may contain).
     * "C" alone means colourless.
     */
    private static void addColorFilter(CardSearchRequest request, List<String> conditions, MapSqlParameterSource params) {
        if (!notBlank(request.colors())) {
            return;
        }
        String upper = request.colors().toUpperCase(Locale.ROOT);
        Set<String> letters = new LinkedHashSet<>();
        for (char c : WUBRG.toCharArray()) {
            if (upper.indexOf(c) >= 0) {
                letters.add(String.valueOf(c));
            }
        }
        boolean colorless = upper.indexOf('C') >= 0;
        if (letters.isEmpty() && !colorless) {
            return; // nothing valid selected
        }
        String mode = lower(request.colorMode()) == null ? "including" : lower(request.colorMode());
        String arrayLiteral = "{" + String.join(",", letters) + "}"; // only W/U/B/R/G can get here

        if (mode.equals("identity")) {
            conditions.add(COLOR_IDENTITY);
            params.addValue("colors", arrayLiteral);
        } else if (letters.isEmpty()) {
            conditions.add(COLORLESS);
        } else {
            conditions.add(mode.equals("exact") ? COLORS_EXACT : COLORS_INCLUDING);
            params.addValue("colors", arrayLiteral);
        }
    }

    private static CardSummary mapSummary(ResultSet rs) throws SQLException {
        return new CardSummary(
            rs.getObject("oracle_id", UUID.class),
            rs.getString("name"),
            rs.getString("mana_cost"),
            rs.getBigDecimal("cmc"),
            rs.getString("type_line"),
            strings(rs.getArray("colors")),
            strings(rs.getArray("color_identity")),
            rs.getObject("default_id", UUID.class),
            rs.getString("set_code"),
            rs.getString("rarity"),
            images(rs),
            prices(rs));
    }

    private static Images images(ResultSet rs) throws SQLException {
        return new Images(rs.getString("img_small"), rs.getString("img_normal"),
            rs.getString("img_large"), rs.getString("img_art"));
    }

    private static Prices prices(ResultSet rs) throws SQLException {
        return new Prices(rs.getBigDecimal("price_usd"), rs.getBigDecimal("price_usd_foil"),
            rs.getBigDecimal("price_usd_etched"), rs.getBigDecimal("price_eur"),
            rs.getBigDecimal("price_eur_foil"), rs.getBigDecimal("price_tix"));
    }

    private static List<String> strings(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        Object[] values = (Object[]) array.getArray();
        return Arrays.stream(values).map(String::valueOf).toList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> parseLegalities(String json) {
        return json == null ? Map.of() : ScryfallJson.MAPPER.readValue(json, Map.class);
    }

    /** "%bolt%" with the user's own % and _ escaped, so they are searched literally. */
    private static String contains(String value) {
        String escaped = value.trim()
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String lower(String value) {
        return notBlank(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }
}
