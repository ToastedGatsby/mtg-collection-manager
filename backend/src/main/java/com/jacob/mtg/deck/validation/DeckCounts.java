package com.jacob.mtg.deck.validation;

import java.util.List;

import com.jacob.mtg.deck.Board;

/** Number of cards per zone (quantities summed). */
public record DeckCounts(int main, int sideboard, int commander) {

    public static DeckCounts of(List<ValidationCard> cards) {
        return new DeckCounts(sum(cards, Board.MAIN), sum(cards, Board.SIDEBOARD), sum(cards, Board.COMMANDER));
    }

    private static int sum(List<ValidationCard> cards, Board board) {
        return cards.stream().filter(c -> c.board() == board).mapToInt(ValidationCard::quantity).sum();
    }
}
