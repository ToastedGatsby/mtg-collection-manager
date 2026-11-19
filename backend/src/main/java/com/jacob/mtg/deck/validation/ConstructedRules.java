package com.jacob.mtg.deck.validation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.jacob.mtg.deck.Board;
import com.jacob.mtg.deck.DeckFormat;
import com.jacob.mtg.deck.validation.LegalityRules.Totals;

/**
 * 60-card constructed formats (Standard, Pioneer, Modern, Legacy, Vintage, Pauper):
 *  - main deck at least 60 cards, sideboard at most 15;
 *  - at most 4 copies of a card across main deck + sideboard (basic lands and cards that say
 *    otherwise excepted), only 1 if the card is "restricted" (Vintage);
 *  - every card must be legal in the format (Scryfall legalities; Pauper's "commons only" rule is
 *    already reflected there, so it needs no special code).
 */
public class ConstructedRules implements FormatRules {

    static final int MIN_MAIN = 60;
    static final int MAX_SIDEBOARD = 15;
    static final int MAX_COPIES = 4;

    private final DeckFormat format;

    public ConstructedRules(DeckFormat format) {
        if (format == DeckFormat.COMMANDER) {
            throw new IllegalArgumentException("Commander has its own rules");
        }
        this.format = format;
    }

    @Override
    public DeckFormat format() {
        return format;
    }

    @Override
    public ValidationResult validate(List<ValidationCard> cards) {
        List<ValidationIssue> issues = new ArrayList<>();
        DeckCounts counts = DeckCounts.of(cards);
        String formatName = format.legalityKey();

        if (counts.commander() > 0) {
            issues.add(ValidationIssue.deck("COMMANDER_ZONE_NOT_ALLOWED",
                formatName + " decks have no commander", counts.commander(), 0));
        }
        if (counts.main() < MIN_MAIN) {
            issues.add(ValidationIssue.deck("DECK_TOO_SMALL",
                "Main deck has " + counts.main() + " cards, needs at least " + MIN_MAIN, counts.main(), MIN_MAIN));
        }
        if (counts.sideboard() > MAX_SIDEBOARD) {
            issues.add(ValidationIssue.deck("SIDEBOARD_TOO_LARGE",
                "Sideboard has " + counts.sideboard() + " cards, at most " + MAX_SIDEBOARD, counts.sideboard(), MAX_SIDEBOARD));
        }

        for (Totals t : LegalityRules.totalsByName(cards, EnumSet.of(Board.MAIN, Board.SIDEBOARD)).values()) {
            ValidationCard card = t.card();
            if (!LegalityRules.checkPlayable(card, formatName, issues)) {
                continue;
            }
            if ("restricted".equals(card.legality())) {
                if (t.quantity() > 1) {
                    issues.add(ValidationIssue.card("RESTRICTED_TOO_MANY",
                        card.name() + " is restricted: at most 1 copy", card.name(), t.quantity(), 1));
                }
                continue;
            }
            int limit = CopyLimits.maxCopies(card, MAX_COPIES);
            if (t.quantity() > limit) {
                issues.add(ValidationIssue.card("TOO_MANY_COPIES",
                    t.quantity() + " copies of " + card.name() + ", at most " + limit, card.name(), t.quantity(), limit));
            }
        }
        return ValidationResult.of(format, counts, issues);
    }
}
