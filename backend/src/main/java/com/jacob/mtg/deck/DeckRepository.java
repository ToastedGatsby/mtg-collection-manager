package com.jacob.mtg.deck;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Every lookup includes the owner's id: a deck that exists but belongs to someone else is "not found". */
public interface DeckRepository extends JpaRepository<Deck, Long> {

    Optional<Deck> findByIdAndUserId(Long id, Long userId);

    List<Deck> findByUserIdOrderByUpdatedAtDesc(Long userId);

    long countByUserId(Long userId);
}
