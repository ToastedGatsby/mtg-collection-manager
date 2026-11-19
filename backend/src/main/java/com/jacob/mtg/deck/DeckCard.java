package com.jacob.mtg.deck;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** N copies of one printing in one zone of a deck. Managed only through its Deck (aggregate root). */
@Entity
@Table(name = "deck_cards")
public class DeckCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @Column(name = "scryfall_id", nullable = false)
    private UUID scryfallId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Board board;

    @Column(nullable = false)
    private int quantity;

    protected DeckCard() {
        // required by JPA
    }

    DeckCard(Deck deck, UUID scryfallId, Board board, int quantity) {
        this.deck = deck;
        this.scryfallId = scryfallId;
        this.board = board;
        this.quantity = quantity;
    }

    boolean matches(UUID scryfallId, Board board) {
        return this.scryfallId.equals(scryfallId) && this.board == board;
    }

    void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public UUID getScryfallId() {
        return scryfallId;
    }

    public Board getBoard() {
        return board;
    }

    public int getQuantity() {
        return quantity;
    }
}
