package com.jacob.mtg.deck;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jacob.mtg.deck.dto.DeckDtos.CreateDeckRequest;
import com.jacob.mtg.deck.dto.DeckDtos.DeckDetail;
import com.jacob.mtg.deck.dto.DeckDtos.DeckSummary;
import com.jacob.mtg.deck.dto.DeckDtos.ImportDeckRequest;
import com.jacob.mtg.deck.dto.DeckDtos.ImportDeckResponse;
import com.jacob.mtg.deck.dto.DeckDtos.SetCardRequest;
import com.jacob.mtg.deck.dto.DeckDtos.UpdateDeckRequest;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

/** The caller's own decks. The user id always comes from the JWT, never from the request. */
@RestController
@RequestMapping("/api/decks")
@SecurityRequirement(name = "bearerAuth")
public class DeckController {

    private final DeckService service;

    public DeckController(DeckService service) {
        this.service = service;
    }

    @GetMapping
    public List<DeckSummary> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(userId(jwt));
    }

    @PostMapping
    public ResponseEntity<DeckDetail> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateDeckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(userId(jwt), request));
    }

    @GetMapping("/{deckId}")
    public DeckDetail get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long deckId) {
        return service.get(userId(jwt), deckId);
    }

    @PatchMapping("/{deckId}")
    public DeckDetail update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long deckId,
                             @Valid @RequestBody UpdateDeckRequest request) {
        return service.update(userId(jwt), deckId, request);
    }

    @DeleteMapping("/{deckId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long deckId) {
        service.delete(userId(jwt), deckId);
        return ResponseEntity.noContent().build();
    }

    /** Idempotent "set quantity" (0 = remove): the same call can be safely retried. */
    @PutMapping("/{deckId}/cards")
    public DeckDetail setCard(@AuthenticationPrincipal Jwt jwt, @PathVariable Long deckId,
                              @Valid @RequestBody SetCardRequest request) {
        return service.setCard(userId(jwt), deckId, request);
    }

    @PostMapping("/import")
    public ResponseEntity<ImportDeckResponse> importDeck(@AuthenticationPrincipal Jwt jwt,
                                                         @Valid @RequestBody ImportDeckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.importDeck(userId(jwt), request));
    }

    @GetMapping(value = "/{deckId}/export", produces = MediaType.TEXT_PLAIN_VALUE)
    public String export(@AuthenticationPrincipal Jwt jwt, @PathVariable Long deckId) {
        return service.export(userId(jwt), deckId);
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
