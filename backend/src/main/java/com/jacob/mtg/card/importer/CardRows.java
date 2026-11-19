package com.jacob.mtg.card.importer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Flat, database-shaped rows produced from one Scryfall card object. */
public final class CardRows {

    private CardRows() {
    }

    /** One row of oracle_cards: the card as a game object (same for every printing). */
    public record OracleRow(
        UUID oracleId,
        String name,
        String manaCost,
        BigDecimal cmc,
        String typeLine,
        String oracleText,
        List<String> colors,
        List<String> colorIdentity,
        List<String> keywords,
        String legalitiesJson) {
    }

    /** One row of card_printings: a physical printing (set, art, prices). */
    public record PrintingRow(
        UUID scryfallId,
        UUID oracleId,
        String setCode,
        String setName,
        String collectorNumber,
        String rarity,
        String lang,
        List<String> finishes,
        String imageUrisJson,
        Integer tcgplayerId,
        Integer cardmarketId,
        LocalDate releasedAt,
        boolean promo,
        String borderColor,
        String purchaseUrisJson,
        BigDecimal priceUsd,
        BigDecimal priceUsdFoil,
        BigDecimal priceUsdEtched,
        BigDecimal priceEur,
        BigDecimal priceEurFoil,
        BigDecimal priceTix) {
    }

    public record ImportRow(OracleRow oracle, PrintingRow printing) {
    }
}
