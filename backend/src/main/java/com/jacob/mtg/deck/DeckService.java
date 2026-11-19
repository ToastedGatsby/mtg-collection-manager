package com.jacob.mtg.deck;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jacob.mtg.common.exception.ApiException;
import com.jacob.mtg.common.exception.NotFoundException;
import com.jacob.mtg.deck.DeckCardQueries.DeckCardView;
import com.jacob.mtg.deck.DeckCardQueries.DeckStats;
import com.jacob.mtg.deck.DeckCardQueries.ResolvedCard;
import com.jacob.mtg.deck.dto.DeckDtos.CreateDeckRequest;
import com.jacob.mtg.deck.dto.DeckDtos.DeckCardDto;
import com.jacob.mtg.deck.dto.DeckDtos.DeckDetail;
import com.jacob.mtg.deck.dto.DeckDtos.DeckSummary;
import com.jacob.mtg.deck.dto.DeckDtos.DeckTotals;
import com.jacob.mtg.deck.dto.DeckDtos.ImportDeckRequest;
import com.jacob.mtg.deck.dto.DeckDtos.ImportDeckResponse;
import com.jacob.mtg.deck.dto.DeckDtos.SetCardRequest;
import com.jacob.mtg.deck.dto.DeckDtos.UpdateDeckRequest;
import com.jacob.mtg.deck.importexport.DeckListExporter;
import com.jacob.mtg.deck.importexport.DeckListExporter.ExportLine;
import com.jacob.mtg.deck.importexport.DeckListParser;
import com.jacob.mtg.deck.importexport.DeckListParser.ParseResult;
import com.jacob.mtg.deck.importexport.DeckListParser.ParsedLine;
import com.jacob.mtg.deck.validation.FormatRulesRegistry;
import com.jacob.mtg.deck.validation.ValidationCard;
import com.jacob.mtg.deck.validation.ValidationResult;

/**
 * Deck use cases. Every method takes the caller's user id (from the JWT) and loads decks with
 * findByIdAndUserId, so a user can never read or change another user's deck by guessing an id.
 */
@Service
@Transactional
public class DeckService {

    static final int MAX_DECKS_PER_USER = 100;
    private static final String DECK_NOT_FOUND = "Deck not found";

    private final DeckRepository decks;
    private final DeckCardQueries queries;
    private final FormatRulesRegistry rules;

    public DeckService(DeckRepository decks, DeckCardQueries queries, FormatRulesRegistry rules) {
        this.decks = decks;
        this.queries = queries;
        this.rules = rules;
    }

    @Transactional(readOnly = true)
    public List<DeckSummary> list(Long userId) {
        List<Deck> owned = decks.findByUserIdOrderByUpdatedAtDesc(userId);
        Map<Long, DeckStats> stats = queries.stats(owned.stream().map(Deck::getId).toList());
        return owned.stream().map(d -> {
            DeckStats s = stats.get(d.getId());
            return new DeckSummary(d.getId(), d.getName(), d.getFormat(),
                s == null ? 0 : s.cardCount(), s == null ? null : s.coverImage(),
                s == null ? null : s.totalEur(), d.getUpdatedAt());
        }).toList();
    }

    public DeckDetail create(Long userId, CreateDeckRequest request) {
        ensureCanCreate(userId);
        Deck deck = decks.save(new Deck(userId, request.name().trim(), request.format(), blankToNull(request.description())));
        return detail(deck);
    }

    @Transactional(readOnly = true)
    public DeckDetail get(Long userId, Long deckId) {
        return detail(load(userId, deckId));
    }

    public DeckDetail update(Long userId, Long deckId, UpdateDeckRequest request) {
        Deck deck = load(userId, deckId);
        deck.update(request.name() == null ? null : request.name().trim(), request.format(), request.description());
        return detail(deck);
    }

    public void delete(Long userId, Long deckId) {
        decks.delete(load(userId, deckId));
    }

    public DeckDetail setCard(Long userId, Long deckId, SetCardRequest request) {
        Deck deck = load(userId, deckId);
        if (request.quantity() > 0 && !queries.printingExists(request.scryfallId())) {
            throw new NotFoundException("Card not found");
        }
        try {
            deck.setQuantity(request.scryfallId(), request.board(), request.quantity());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        }
        return detail(deck);
    }

    /**
     * Creates a deck from a pasted text list. Lines that cannot be read or cards that cannot be
     * found are reported back instead of failing the whole import.
     */
    public ImportDeckResponse importDeck(Long userId, ImportDeckRequest request) {
        ParseResult parsed = DeckListParser.parse(request.list());
        if (parsed.lines().isEmpty()) {
            throw ApiException.badRequest(String.join("; ", parsed.errors()));
        }
        ensureCanCreate(userId);

        Map<String, ResolvedCard> resolved = queries.resolveNames(parsed.lines().stream().map(ParsedLine::name).toList());
        Deck deck = new Deck(userId, request.name().trim(), request.format(), null);
        Set<String> unresolved = new LinkedHashSet<>();

        for (ParsedLine line : parsed.lines()) {
            ResolvedCard card = resolved.get(line.name().toLowerCase(Locale.ROOT));
            if (card == null) {
                unresolved.add(line.name());
                continue;
            }
            UUID printing = card.defaultScryfallId();
            if (line.setCode() != null) {
                printing = queries.findPrinting(card.oracleId(), line.setCode(), line.collectorNumber())
                    .orElse(card.defaultScryfallId()); // unknown set -> keep the card, use its default printing
            }
            try {
                deck.addQuantity(printing, line.board(), line.quantity());
            } catch (IllegalArgumentException e) {
                throw ApiException.badRequest(e.getMessage());
            }
        }
        return new ImportDeckResponse(detail(decks.save(deck)), List.copyOf(unresolved), parsed.errors());
    }

    @Transactional(readOnly = true)
    public String export(Long userId, Long deckId) {
        Deck deck = load(userId, deckId);
        List<ExportLine> lines = queries.findCards(deck.getId(), deck.getFormat()).stream()
            .map(c -> new ExportLine(c.board(), c.quantity(), c.name(), c.setCode(), c.collectorNumber()))
            .toList();
        return DeckListExporter.export(lines);
    }

    // ---------- helpers ----------

    private Deck load(Long userId, Long deckId) {
        return decks.findByIdAndUserId(deckId, userId).orElseThrow(() -> new NotFoundException(DECK_NOT_FOUND));
    }

    private void ensureCanCreate(Long userId) {
        if (decks.countByUserId(userId) >= MAX_DECKS_PER_USER) {
            throw ApiException.badRequest("You can have at most " + MAX_DECKS_PER_USER + " decks");
        }
    }

    /**
     * Builds the response: cards (read with SQL joined to the catalogue), validation for the deck's
     * format and value totals. flush() first so the SQL read sees the changes JPA has not written yet.
     */
    private DeckDetail detail(Deck deck) {
        decks.flush();
        List<DeckCardView> views = queries.findCards(deck.getId(), deck.getFormat());

        List<ValidationCard> validationCards = views.stream()
            .map(v -> new ValidationCard(v.name(), v.typeLine(), v.oracleText(), v.colorIdentity(), v.keywords(),
                v.legality(), v.board(), v.quantity()))
            .toList();
        ValidationResult validation = rules.forFormat(deck.getFormat()).validate(validationCards);

        List<DeckCardDto> cards = views.stream()
            .map(v -> new DeckCardDto(v.scryfallId(), v.oracleId(), v.board(), v.quantity(), v.name(), v.manaCost(),
                v.cmc(), v.typeLine(), v.colors(), v.colorIdentity(), v.setCode(), v.collectorNumber(), v.rarity(),
                v.images(), v.prices()))
            .toList();

        return new DeckDetail(deck.getId(), deck.getName(), deck.getFormat(), deck.getDescription(),
            deck.getCreatedAt(), deck.getUpdatedAt(), cards, validation, totals(views));
    }

    private static DeckTotals totals(List<DeckCardView> views) {
        BigDecimal eur = BigDecimal.ZERO;
        BigDecimal usd = BigDecimal.ZERO;
        int withoutEur = 0;
        for (DeckCardView v : views) {
            if (v.board() == Board.MAYBEBOARD) {
                continue;
            }
            BigDecimal qty = BigDecimal.valueOf(v.quantity());
            if (v.prices().eur() != null) {
                eur = eur.add(v.prices().eur().multiply(qty));
            } else {
                withoutEur += v.quantity();
            }
            if (v.prices().usd() != null) {
                usd = usd.add(v.prices().usd().multiply(qty));
            }
        }
        return new DeckTotals(eur, usd, withoutEur);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
