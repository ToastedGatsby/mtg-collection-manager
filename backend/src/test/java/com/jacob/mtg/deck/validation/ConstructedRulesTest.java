package com.jacob.mtg.deck.validation;

import static com.jacob.mtg.deck.validation.TestCards.basicMountain;
import static com.jacob.mtg.deck.validation.TestCards.card;
import static com.jacob.mtg.deck.validation.TestCards.codes;
import static com.jacob.mtg.deck.validation.TestCards.sixtyCards;
import static com.jacob.mtg.deck.validation.TestCards.withLegality;
import static com.jacob.mtg.deck.validation.TestCards.withText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.jacob.mtg.deck.Board;
import com.jacob.mtg.deck.DeckFormat;

class ConstructedRulesTest {

    private final ConstructedRules modern = new ConstructedRules(DeckFormat.MODERN);
    private final ConstructedRules vintage = new ConstructedRules(DeckFormat.VINTAGE);

    @Test
    void sixtyLegalCards_isLegal() {
        ValidationResult result = modern.validate(sixtyCards());
        assertThat(result.legal()).isTrue();
        assertThat(result.mainCount()).isEqualTo(60);
    }

    @Test
    void fewerThanSixty_isTooSmall() {
        ValidationResult result = modern.validate(List.of(card("Lightning Bolt", 4)));
        assertThat(codes(result)).containsExactly("DECK_TOO_SMALL");
        assertThat(result.issues().get(0).actual()).isEqualTo(4);
    }

    @Test
    void moreThanFifteenInSideboard() {
        List<ValidationCard> cards = new ArrayList<>(sixtyCards());
        for (int i = 0; i < 4; i++) {
            cards.add(card("Side " + i, 4, Board.SIDEBOARD)); // 16 cards
        }
        assertThat(codes(modern.validate(cards))).containsExactly("SIDEBOARD_TOO_LARGE");
    }

    @Test
    void fifthCopy_counted_acrossMainAndSideboard() {
        List<ValidationCard> cards = new ArrayList<>(sixtyCards());
        cards.add(card("Card 0", 1, Board.SIDEBOARD)); // 4 in main + 1 in sideboard
        ValidationResult result = modern.validate(cards);
        assertThat(codes(result)).containsExactly("TOO_MANY_COPIES");
        assertThat(result.issues().get(0).cardName()).isEqualTo("Card 0");
    }

    @Test
    void maybeboard_isIgnored() {
        List<ValidationCard> cards = new ArrayList<>(sixtyCards());
        cards.add(card("Card 0", 10, Board.MAYBEBOARD));
        assertThat(modern.validate(cards).legal()).isTrue();
    }

    @Test
    void basicLands_anyNumber() {
        List<ValidationCard> cards = new ArrayList<>(sixtyCards().subList(0, 10)); // 40 cards
        cards.add(basicMountain(20));
        assertThat(modern.validate(cards).legal()).isTrue();
    }

    @Test
    void cardsThatSayAnyNumber_orUpToSeven() {
        List<ValidationCard> cards = new ArrayList<>(sixtyCards().subList(0, 5)); // 20 cards
        cards.add(withText("Relentless Rats", 33, "A deck can have any number of cards named Relentless Rats."));
        cards.add(withText("Seven Dwarves", 7, "A deck can have up to seven cards named Seven Dwarves."));
        assertThat(modern.validate(cards).legal()).isTrue();

        cards.add(withText("Seven Dwarves", 1, "A deck can have up to seven cards named Seven Dwarves."));
        assertThat(codes(modern.validate(cards))).containsExactly("TOO_MANY_COPIES");
    }

    @Test
    void bannedAndNotLegalCards() {
        List<ValidationCard> cards = new ArrayList<>(sixtyCards());
        cards.add(withLegality("Banned Card", 1, "banned"));
        cards.add(withLegality("Too Old Card", 1, "not_legal"));
        assertThat(codes(modern.validate(cards))).containsExactly("BANNED", "NOT_LEGAL");
    }

    @Test
    void vintage_restrictedCard_onlyOneCopy() {
        List<ValidationCard> cards = new ArrayList<>(sixtyCards());
        cards.add(withLegality("Sol Ring", 1, "restricted"));
        assertThat(vintage.validate(cards).legal()).isTrue();

        cards.add(withLegality("Sol Ring", 1, "restricted"));
        assertThat(codes(vintage.validate(cards))).containsExactly("RESTRICTED_TOO_MANY");
    }

    @Test
    void commanderZone_notAllowedInConstructed() {
        List<ValidationCard> cards = new ArrayList<>(sixtyCards());
        cards.add(card("Some Legend", 1, Board.COMMANDER));
        assertThat(codes(modern.validate(cards))).containsExactly("COMMANDER_ZONE_NOT_ALLOWED");
    }

    @Test
    void cannotBeBuiltForCommander() {
        assertThatThrownBy(() -> new ConstructedRules(DeckFormat.COMMANDER)).isInstanceOf(IllegalArgumentException.class);
    }
}
