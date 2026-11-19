package com.jacob.mtg.deck;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Aggregate root: cards are only added/changed through the deck, which keeps the invariants
 * (quantity 1..250, one row per printing+zone, entry limit) in one place.
 */
@Entity
@Table(name = "decks")
public class Deck {

    public static final int MAX_QUANTITY = 250;
    public static final int MAX_ENTRIES = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owner. Every query filters on it, so nobody can read or edit someone else's deck. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeckFormat format;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Visibility visibility = Visibility.PRIVATE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeckCard> cards = new ArrayList<>();

    protected Deck() {
        // required by JPA
    }

    public Deck(Long userId, String name, DeckFormat format, String description) {
        this.userId = userId;
        this.name = name;
        this.format = format;
        this.description = description;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    /**
     * Sets how many copies of a printing are in a zone; 0 removes it.
     *
     * @throws IllegalArgumentException if the quantity is out of range or the deck is full
     */
    public void setQuantity(UUID scryfallId, Board board, int quantity) {
        if (quantity < 0 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity must be between 0 and " + MAX_QUANTITY);
        }
        Optional<DeckCard> existing = find(scryfallId, board);
        if (quantity == 0) {
            existing.ifPresent(cards::remove); // orphanRemoval deletes the row
        } else if (existing.isPresent()) {
            existing.get().setQuantity(quantity);
        } else {
            if (cards.size() >= MAX_ENTRIES) {
                throw new IllegalArgumentException("A deck can have at most " + MAX_ENTRIES + " different entries");
            }
            cards.add(new DeckCard(this, scryfallId, board, quantity));
        }
        touch();
    }

    /** Adds copies (used by import, where the same card can appear on several lines). */
    public void addQuantity(UUID scryfallId, Board board, int quantity) {
        int current = find(scryfallId, board).map(DeckCard::getQuantity).orElse(0);
        setQuantity(scryfallId, board, Math.min(MAX_QUANTITY, current + quantity));
    }

    public void update(String name, DeckFormat format, String description) {
        if (name != null) {
            this.name = name;
        }
        if (format != null) {
            this.format = format;
        }
        if (description != null) {
            this.description = description.isBlank() ? null : description;
        }
        touch();
    }

    private Optional<DeckCard> find(UUID scryfallId, Board board) {
        return cards.stream().filter(c -> c.matches(scryfallId, board)).findFirst();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public DeckFormat getFormat() {
        return format;
    }

    public String getDescription() {
        return description;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<DeckCard> getCards() {
        return Collections.unmodifiableList(cards);
    }
}
