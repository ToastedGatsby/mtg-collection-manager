package com.jacob.mtg.card.scryfall;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The subset of a Scryfall card object we import (https://scryfall.com/docs/api/cards).
 * Wrapper types (Boolean, Double) on purpose: Jackson 3 fails on null for primitives.
 * Prices and dates stay Strings here and are parsed in ScryfallCardMapper.
 */
public record ScryfallCard(
    UUID id,
    UUID oracleId,
    String name,
    String layout,
    String manaCost,
    Double cmc,
    String typeLine,
    String oracleText,
    List<String> colors,
    List<String> colorIdentity,
    List<String> keywords,
    Map<String, String> legalities,
    String set,
    String setName,
    String collectorNumber,
    String rarity,
    String lang,
    List<String> finishes,
    Map<String, String> imageUris,
    List<Face> cardFaces,
    Integer tcgplayerId,
    Integer cardmarketId,
    String releasedAt,
    Prices prices,
    Map<String, String> purchaseUris,
    Boolean digital,
    Boolean promo,
    Boolean oversized,
    String borderColor) {

    /** One side of a double-faced / split / adventure card. */
    public record Face(
        String name,
        UUID oracleId,
        String manaCost,
        String typeLine,
        String oracleText,
        List<String> colors,
        Map<String, String> imageUris) {
    }

    /** Scryfall sends prices as strings ("0.25") or null. */
    public record Prices(String usd, String usdFoil, String usdEtched, String eur, String eurFoil, String tix) {
    }
}
