package com.jacob.mtg.card.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Response shapes of the card API. */
public final class CardDtos {

    private CardDtos() {
    }

    /** USD = TCGplayer, EUR = Cardmarket, TIX = Cardhoarder (MTGO), as published by Scryfall. */
    public record Prices(BigDecimal usd, BigDecimal usdFoil, BigDecimal usdEtched,
                         BigDecimal eur, BigDecimal eurFoil, BigDecimal tix) {
    }

    public record Images(String small, String normal, String large, String artCrop) {
    }

    /** One row of the search grid: the card plus its default printing (image + price). */
    public record CardSummary(
        UUID oracleId,
        String name,
        String manaCost,
        BigDecimal cmc,
        String typeLine,
        List<String> colors,
        List<String> colorIdentity,
        UUID defaultPrintingId,
        String setCode,
        String rarity,
        Images images,
        Prices prices) {
    }

    /** Full card: rules text and legality in every format. */
    public record CardDetail(
        UUID oracleId,
        String name,
        String manaCost,
        BigDecimal cmc,
        String typeLine,
        String oracleText,
        List<String> colors,
        List<String> colorIdentity,
        List<String> keywords,
        Map<String, String> legalities,
        UUID defaultPrintingId,
        Images images) {
    }

    /** One physical printing, for the "which version?" panel. */
    public record Printing(
        UUID scryfallId,
        String setCode,
        String setName,
        String collectorNumber,
        String rarity,
        String lang,
        LocalDate releasedAt,
        List<String> finishes,
        boolean promo,
        Images images,
        Prices prices,
        String tcgplayerUrl,
        String cardmarketUrl) {
    }
}
