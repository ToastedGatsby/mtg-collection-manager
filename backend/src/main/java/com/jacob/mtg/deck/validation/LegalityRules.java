package com.jacob.mtg.deck.validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jacob.mtg.deck.Board;

/** Helpers shared by the format strategies. */
final class LegalityRules {

    private LegalityRules() {
    }

    /** Cards grouped by name with quantities summed over the given zones (several printings = same card). */
    static Map<String, Totals> totalsByName(List<ValidationCard> cards, Set<Board> boards) {
        Map<String, Totals> totals = new LinkedHashMap<>();
        for (ValidationCard card : cards) {
            if (boards.contains(card.board())) {
                totals.merge(card.name(), new Totals(card, card.quantity()),
                    (a, b) -> new Totals(a.card(), a.quantity() + b.quantity()));
            }
        }
        return totals;
    }

    /** Adds a BANNED / NOT_LEGAL issue if the card cannot be played in the format at all. */
    static boolean checkPlayable(ValidationCard card, String formatName, List<ValidationIssue> issues) {
        String legality = card.legality() == null ? "not_legal" : card.legality();
        switch (legality) {
            case "legal", "restricted" -> {
                return true;
            }
            case "banned" -> issues.add(ValidationIssue.card("BANNED",
                card.name() + " is banned in " + formatName, card.name()));
            default -> issues.add(ValidationIssue.card("NOT_LEGAL",
                card.name() + " is not legal in " + formatName, card.name()));
        }
        return false;
    }

    record Totals(ValidationCard card, int quantity) {
    }
}
