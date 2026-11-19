package com.jacob.mtg.deck.validation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.jacob.mtg.deck.Board;
import com.jacob.mtg.deck.DeckFormat;
import com.jacob.mtg.deck.validation.LegalityRules.Totals;

/**
 * Commander (EDH):
 *  - exactly 100 cards including the commander(s); the sideboard is not used;
 *  - 1 commander, or 2 with a pairing ability (Partner, Partner with, Friends forever,
 *    Choose a Background + a Background, Doctor's companion + a Time Lord Doctor);
 *  - a commander must be a legendary creature or say "can be your commander";
 *  - singleton: 1 copy per card (basic lands and "any number" cards excepted);
 *  - every card's colour identity must fit inside the commanders' combined identity;
 *  - every card must be legal (not banned) in Commander.
 */
public class CommanderRules implements FormatRules {

    static final int DECK_SIZE = 100;

    @Override
    public DeckFormat format() {
        return DeckFormat.COMMANDER;
    }

    @Override
    public ValidationResult validate(List<ValidationCard> cards) {
        List<ValidationIssue> issues = new ArrayList<>();
        DeckCounts counts = DeckCounts.of(cards);
        String formatName = DeckFormat.COMMANDER.legalityKey();

        List<ValidationCard> commanders = cards.stream().filter(c -> c.board() == Board.COMMANDER).toList();
        checkCommanders(commanders, counts.commander(), issues);

        int total = counts.main() + counts.commander();
        if (total != DECK_SIZE) {
            issues.add(ValidationIssue.deck("WRONG_DECK_SIZE",
                "Commander decks have exactly " + DECK_SIZE + " cards (commander included), this one has " + total,
                total, DECK_SIZE));
        }

        Set<String> identity = new HashSet<>();
        commanders.forEach(c -> identity.addAll(c.colorIdentity()));

        for (Totals t : LegalityRules.totalsByName(cards, EnumSet.of(Board.MAIN, Board.COMMANDER)).values()) {
            ValidationCard card = t.card();
            LegalityRules.checkPlayable(card, formatName, issues);

            int limit = CopyLimits.maxCopies(card, 1);
            if (t.quantity() > limit) {
                issues.add(ValidationIssue.card("TOO_MANY_COPIES",
                    "Commander is singleton: " + t.quantity() + " copies of " + card.name() + ", at most " + limit,
                    card.name(), t.quantity(), limit));
            }
            // only meaningful once a commander has been chosen
            if (!commanders.isEmpty() && !identity.containsAll(card.colorIdentity())) {
                issues.add(ValidationIssue.card("COLOR_IDENTITY",
                    card.name() + " is outside the commander's colour identity " + identity, card.name()));
            }
        }
        return ValidationResult.of(DeckFormat.COMMANDER, counts, issues);
    }

    private static void checkCommanders(List<ValidationCard> commanders, int count, List<ValidationIssue> issues) {
        if (count == 0) {
            issues.add(ValidationIssue.deck("NO_COMMANDER", "Choose a commander", 0, 1));
            return;
        }
        if (count > 2) {
            issues.add(ValidationIssue.deck("TOO_MANY_COMMANDERS", "At most 2 commanders", count, 2));
            return;
        }
        if (count == 1) {
            ValidationCard only = commanders.get(0);
            if (!canBeCommander(only)) {
                issues.add(ValidationIssue.card("INVALID_COMMANDER",
                    only.name() + " cannot be a commander (not a legendary creature)", only.name()));
            }
            return;
        }
        // two commanders: either two different cards, or the same card twice (always invalid)
        if (commanders.size() == 1) {
            issues.add(ValidationIssue.card("INVALID_PARTNERS",
                "The same card cannot be both commanders", commanders.get(0).name()));
            return;
        }
        ValidationCard a = commanders.get(0);
        ValidationCard b = commanders.get(1);
        if (!isValidPair(a, b) && !isValidPair(b, a)) {
            issues.add(ValidationIssue.card("INVALID_PARTNERS",
                a.name() + " and " + b.name() + " cannot be commanders together", a.name()));
            return;
        }
        for (ValidationCard c : commanders) {
            boolean background = c.frontTypeContains("background");
            if (!background && !canBeCommander(c)) {
                issues.add(ValidationIssue.card("INVALID_COMMANDER",
                    c.name() + " cannot be a commander (not a legendary creature)", c.name()));
            }
        }
    }

    static boolean canBeCommander(ValidationCard card) {
        boolean legendaryCreature = card.frontTypeContains("legendary") && card.frontTypeContains("creature");
        return legendaryCreature || card.oracleText().toLowerCase(Locale.ROOT).contains("can be your commander");
    }

    /** Is (a, b) a legal pair, from a's point of view? Called both ways. */
    static boolean isValidPair(ValidationCard a, ValidationCard b) {
        if (a.hasKeyword("Partner") && b.hasKeyword("Partner")) {
            return true;
        }
        if (a.hasKeyword("Partner with")) {
            // "Partner with Pir, Imaginative Rascal" must name the other commander
            return a.oracleText().contains("Partner with " + b.name());
        }
        if (a.hasKeyword("Friends forever") && b.hasKeyword("Friends forever")) {
            return true;
        }
        if (a.hasKeyword("Choose a Background") && b.frontTypeContains("background")) {
            return true;
        }
        return a.hasKeyword("Doctor's companion") && b.frontTypeContains("time lord") && b.frontTypeContains("doctor");
    }
}
