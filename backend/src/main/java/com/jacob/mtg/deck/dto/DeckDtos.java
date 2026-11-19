package com.jacob.mtg.deck.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.jacob.mtg.card.dto.CardDtos.Images;
import com.jacob.mtg.card.dto.CardDtos.Prices;
import com.jacob.mtg.deck.Board;
import com.jacob.mtg.deck.DeckFormat;
import com.jacob.mtg.deck.validation.ValidationResult;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Requests and responses of the deck API. */
public final class DeckDtos {

    private DeckDtos() {
    }

    // ---------- requests ----------

    public record CreateDeckRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull DeckFormat format,
        @Size(max = 2000) String description) {
    }

    /** Every field optional: only what is sent changes. */
    public record UpdateDeckRequest(
        @Size(min = 1, max = 100) String name,
        DeckFormat format,
        @Size(max = 2000) String description) {
    }

    /** Sets the number of copies of a printing in a zone; 0 removes it. */
    public record SetCardRequest(
        @NotNull UUID scryfallId,
        @NotNull Board board,
        @Min(0) @Max(250) int quantity) {
    }

    public record ImportDeckRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull DeckFormat format,
        @NotBlank @Size(max = 50_000) String list) {
    }

    // ---------- responses ----------

    public record DeckSummary(Long id, String name, DeckFormat format, int cardCount, String coverImage,
                              BigDecimal totalEur, Instant updatedAt) {
    }

    public record DeckCardDto(
        UUID scryfallId,
        UUID oracleId,
        Board board,
        int quantity,
        String name,
        String manaCost,
        BigDecimal cmc,
        String typeLine,
        List<String> colors,
        List<String> colorIdentity,
        String setCode,
        String collectorNumber,
        String rarity,
        Images images,
        Prices prices) {
    }

    /** Deck value: main + sideboard + commander (maybeboard excluded). */
    public record DeckTotals(BigDecimal eur, BigDecimal usd, int cardsWithoutEurPrice) {
    }

    public record DeckDetail(
        Long id,
        String name,
        DeckFormat format,
        String description,
        Instant createdAt,
        Instant updatedAt,
        List<DeckCardDto> cards,
        ValidationResult validation,
        DeckTotals totals) {
    }

    public record ImportDeckResponse(DeckDetail deck, List<String> unresolvedCards, List<String> parseErrors) {
    }
}
