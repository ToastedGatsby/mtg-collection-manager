package com.jacob.mtg.card.importer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.jacob.mtg.card.importer.CardRows.ImportRow;
import com.jacob.mtg.card.importer.CardRows.OracleRow;
import com.jacob.mtg.card.importer.CardRows.PrintingRow;
import com.jacob.mtg.card.scryfall.ScryfallCard;
import com.jacob.mtg.card.scryfall.ScryfallCard.Face;
import com.jacob.mtg.card.scryfall.ScryfallJson;

/**
 * Turns a Scryfall card into database rows. Pure function (no I/O) -> easy to unit test.
 *
 * Double-faced cards (e.g. Delver of Secrets) have no top-level mana cost / text / image:
 * those live in card_faces, so we fall back to the faces.
 */
public final class ScryfallCardMapper {

    /** Not real deck cards: tokens, emblems, art cards, oversized variant-format cards. */
    private static final Set<String> SKIPPED_LAYOUTS = Set.of(
        "token", "double_faced_token", "emblem", "art_series", "vanguard", "planar", "scheme");

    private ScryfallCardMapper() {
    }

    public static Optional<ImportRow> map(ScryfallCard card) {
        if (card.id() == null
            || Boolean.TRUE.equals(card.digital())       // Arena/MTGO-only cards: no paper collection
            || Boolean.TRUE.equals(card.oversized())
            || (card.layout() != null && SKIPPED_LAYOUTS.contains(card.layout()))) {
            return Optional.empty();
        }

        List<Face> faces = card.cardFaces() == null ? List.of() : card.cardFaces();
        Face front = faces.isEmpty() ? null : faces.get(0);

        UUID oracleId = card.oracleId() != null ? card.oracleId() : (front != null ? front.oracleId() : null);
        if (oracleId == null || card.name() == null || card.set() == null) {
            return Optional.empty();
        }

        OracleRow oracle = new OracleRow(
            oracleId,
            card.name(),
            firstNonBlank(card.manaCost(), joinFaces(faces, Face::manaCost, " // ")),
            card.cmc() == null ? BigDecimal.ZERO : BigDecimal.valueOf(card.cmc()),
            firstNonBlank(card.typeLine(), joinFaces(faces, Face::typeLine, " // ")),
            firstNonBlank(card.oracleText(), joinFaces(faces, Face::oracleText, "\n//\n")),
            orEmpty(card.colors() != null ? card.colors() : (front != null ? front.colors() : null)),
            orEmpty(card.colorIdentity()),
            orEmpty(card.keywords()),
            toJson(card.legalities() == null ? Map.of() : card.legalities()));

        ScryfallCard.Prices prices = card.prices();
        PrintingRow printing = new PrintingRow(
            card.id(),
            oracleId,
            card.set(),
            card.setName() == null ? card.set().toUpperCase() : card.setName(),
            card.collectorNumber() == null ? "" : card.collectorNumber(),
            card.rarity() == null ? "common" : card.rarity(),
            card.lang() == null ? "en" : card.lang(),
            orEmpty(card.finishes()),
            nullableJson(card.imageUris() != null ? card.imageUris() : (front != null ? front.imageUris() : null)),
            card.tcgplayerId(),
            card.cardmarketId(),
            parseDate(card.releasedAt()),
            Boolean.TRUE.equals(card.promo()),
            card.borderColor(),
            nullableJson(card.purchaseUris()),
            prices == null ? null : price(prices.usd()),
            prices == null ? null : price(prices.usdFoil()),
            prices == null ? null : price(prices.usdEtched()),
            prices == null ? null : price(prices.eur()),
            prices == null ? null : price(prices.eurFoil()),
            prices == null ? null : price(prices.tix()));

        return Optional.of(new ImportRow(oracle, printing));
    }

    private static String joinFaces(List<Face> faces, Function<Face, String> field, String separator) {
        String joined = faces.stream()
            .map(field)
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.joining(separator));
        return joined.isEmpty() ? null : joined;
    }

    private static String firstNonBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private static List<String> orEmpty(List<String> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
    }

    static BigDecimal price(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null; // a malformed price must not stop a 100k-card import
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return value == null ? null : LocalDate.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String nullableJson(Map<String, String> map) {
        return map == null || map.isEmpty() ? null : toJson(map);
    }

    private static String toJson(Object value) {
        return ScryfallJson.MAPPER.writeValueAsString(value);
    }
}
