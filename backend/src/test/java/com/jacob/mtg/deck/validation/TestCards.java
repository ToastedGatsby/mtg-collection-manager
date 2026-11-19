package com.jacob.mtg.deck.validation;

import java.util.ArrayList;
import java.util.List;

import com.jacob.mtg.deck.Board;

/** Small builder so rule tests read like a deck list. */
final class TestCards {

    private TestCards() {
    }

    static ValidationCard card(String name, int qty) {
        return new ValidationCard(name, "Instant", "", List.of("R"), List.of(), "legal", Board.MAIN, qty);
    }

    static ValidationCard card(String name, int qty, Board board) {
        return new ValidationCard(name, "Instant", "", List.of("R"), List.of(), "legal", board, qty);
    }

    static ValidationCard withLegality(String name, int qty, String legality) {
        return new ValidationCard(name, "Instant", "", List.of("R"), List.of(), legality, Board.MAIN, qty);
    }

    static ValidationCard basicMountain(int qty) {
        return new ValidationCard("Mountain", "Basic Land — Mountain", "({T}: Add {R}.)", List.of(), List.of(), "legal", Board.MAIN, qty);
    }

    static ValidationCard withText(String name, int qty, String oracleText) {
        return new ValidationCard(name, "Creature — Rat", oracleText, List.of("B"), List.of(), "legal", Board.MAIN, qty);
    }

    static ValidationCard commander(String name, List<String> identity, String... keywords) {
        return new ValidationCard(name, "Legendary Creature — Human", "", identity, List.of(keywords), "legal", Board.COMMANDER, 1);
    }

    static ValidationCard edhCard(String name, List<String> identity) {
        return new ValidationCard(name, "Sorcery", "", identity, List.of(), "legal", Board.MAIN, 1);
    }

    /** n different legal one-ofs, e.g. to pad a deck to a given size. */
    static List<ValidationCard> filler(int n, List<String> identity) {
        List<ValidationCard> cards = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            cards.add(edhCard("Filler " + i, identity));
        }
        return cards;
    }

    /** A legal 60-card constructed main deck: 15 playsets. */
    static List<ValidationCard> sixtyCards() {
        List<ValidationCard> cards = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            cards.add(card("Card " + i, 4));
        }
        return cards;
    }

    static List<String> codes(ValidationResult result) {
        return result.issues().stream().map(ValidationIssue::code).toList();
    }
}
