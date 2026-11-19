package com.jacob.mtg.card;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jacob.mtg.card.dto.CardDtos.CardDetail;
import com.jacob.mtg.card.dto.CardDtos.CardSummary;
import com.jacob.mtg.card.dto.CardDtos.Printing;
import com.jacob.mtg.common.exception.NotFoundException;
import com.jacob.mtg.common.web.PageResponse;

/** Public, read-only card catalogue (GET /api/cards/** is permitAll in SecurityConfig). */
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardSearchRepository cards;

    public CardController(CardSearchRepository cards) {
        this.cards = cards;
    }

    /** Example: /api/cards?q=bolt&colors=R&format=modern&sort=cmc&page=0&size=40 */
    @GetMapping
    public PageResponse<CardSummary> search(CardSearchRequest request) {
        return cards.search(request);
    }

    @GetMapping("/{oracleId}")
    public CardDetail detail(@PathVariable UUID oracleId) {
        return cards.findDetail(oracleId).orElseThrow(() -> new NotFoundException("Card not found"));
    }

    @GetMapping("/{oracleId}/printings")
    public List<Printing> printings(@PathVariable UUID oracleId) {
        return cards.findPrintings(oracleId);
    }
}
