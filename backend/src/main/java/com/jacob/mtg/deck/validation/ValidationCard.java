package com.jacob.mtg.deck.validation;

import java.util.List;
import java.util.Locale;

import com.jacob.mtg.deck.Board;

/**
 * Everything the rules need to know about one deck entry. Deliberately independent of JPA and
 * the database, so the rules are plain Java that unit tests can feed directly.
 *
 * @param legality the card's legality in the deck's format: legal | not_legal | banned | restricted
 */
public record ValidationCard(
    String name,
    String typeLine,
    String oracleText,
    List<String> colorIdentity,
    List<String> keywords,
    String legality,
    Board board,
    int quantity) {

    public ValidationCard {
        colorIdentity = colorIdentity == null ? List.of() : List.copyOf(colorIdentity);
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        typeLine = typeLine == null ? "" : typeLine;
        oracleText = oracleText == null ? "" : oracleText;
    }

    /** Type line of the front face ("Legendary Creature — Human // Legendary Planeswalker" -> first half). */
    public String frontTypeLine() {
        int split = typeLine.indexOf(" // ");
        return split < 0 ? typeLine : typeLine.substring(0, split);
    }

    public boolean hasKeyword(String keyword) {
        return keywords.stream().anyMatch(k -> k.equalsIgnoreCase(keyword));
    }

    public boolean frontTypeContains(String word) {
        return frontTypeLine().toLowerCase(Locale.ROOT).contains(word.toLowerCase(Locale.ROOT));
    }
}
