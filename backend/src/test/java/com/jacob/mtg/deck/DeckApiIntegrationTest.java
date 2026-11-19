package com.jacob.mtg.deck;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.jacob.mtg.card.importer.CardImportService;
import com.jayway.jsonpath.JsonPath;

/**
 * Deck API end to end (security filter chain + PostgreSQL), with two users to prove that
 * one user cannot touch the other's decks. Cards come from the Scryfall test fixture.
 */
@SpringBootTest
@Transactional
class DeckApiIntegrationTest {

    // printings from src/test/resources/scryfall/sample-cards.json
    private static final String BOLT = printing(1);
    private static final String COUNTERSPELL = printing(3);
    private static final String RING = printing(6);
    private static final String DRAGON = printing(10);

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CardImportService importService;

    private MockMvc mvc;
    private String alice;
    private String bob;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        try (InputStream json = getClass().getResourceAsStream("/scryfall/sample-cards.json")) {
            importService.importFrom(json);
        }
        alice = registerAndGetToken();
        bob = registerAndGetToken();
    }

    @Test
    void commanderDeck_isValidated_andPriced() throws Exception {
        long deckId = createDeck(alice, "Dragons", "COMMANDER");
        setCard(alice, deckId, DRAGON, "COMMANDER", 1);
        setCard(alice, deckId, BOLT, "MAIN", 1);
        setCard(alice, deckId, RING, "MAIN", 1);
        setCard(alice, deckId, COUNTERSPELL, "MAIN", 1);

        mvc.perform(auth(get("/api/decks/" + deckId), alice))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cards", hasSize(4)))
            .andExpect(jsonPath("$.validation.legal").value(false))
            .andExpect(jsonPath("$.validation.commanderCount").value(1))
            // blue Counterspell in a mono-red commander deck, and 4 cards instead of 100
            .andExpect(jsonPath("$.validation.issues[*].code", containsInAnyOrder("WRONG_DECK_SIZE", "COLOR_IDENTITY")))
            .andExpect(jsonPath("$.validation.issues[?(@.code == 'COLOR_IDENTITY')].cardName").value("Testcard Counterspell"))
            .andExpect(jsonPath("$.totals.eur").value(25.30));
    }

    @Test
    void constructedDeck_fifthCopyAndTooSmall() throws Exception {
        long deckId = createDeck(alice, "Burn", "MODERN");
        mvc.perform(auth(put("/api/decks/" + deckId + "/cards"), alice).contentType(MediaType.APPLICATION_JSON)
                .content(cardJson(BOLT, "MAIN", 5)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.validation.issues[*].code", containsInAnyOrder("DECK_TOO_SMALL", "TOO_MANY_COPIES")));
    }

    @Test
    void quantityZero_removesTheCard() throws Exception {
        long deckId = createDeck(alice, "Burn", "MODERN");
        setCard(alice, deckId, BOLT, "MAIN", 4);
        mvc.perform(auth(put("/api/decks/" + deckId + "/cards"), alice).contentType(MediaType.APPLICATION_JSON)
                .content(cardJson(BOLT, "MAIN", 0)))
            .andExpect(jsonPath("$.cards", hasSize(0)));
    }

    @Test
    void otherUsers_cannotSeeOrChangeMyDeck() throws Exception {
        long deckId = createDeck(alice, "Secret tech", "MODERN");

        mvc.perform(auth(get("/api/decks/" + deckId), bob)).andExpect(status().isNotFound());
        mvc.perform(auth(put("/api/decks/" + deckId + "/cards"), bob).contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(BOLT, "MAIN", 4))).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/decks/" + deckId), bob)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/decks"), bob)).andExpect(jsonPath("$", hasSize(0)));

        // still there, untouched, for its owner
        mvc.perform(auth(get("/api/decks/" + deckId), alice))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cards", hasSize(0)));
    }

    @Test
    void listShowsSummaries() throws Exception {
        long deckId = createDeck(alice, "Dragons", "COMMANDER");
        setCard(alice, deckId, DRAGON, "COMMANDER", 1);
        setCard(alice, deckId, BOLT, "MAIN", 1);

        mvc.perform(auth(get("/api/decks"), alice))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].cardCount").value(2))
            .andExpect(jsonPath("$[0].coverImage", containsString("art_crop")));
    }

    @Test
    void renameAndChangeFormat_thenDelete() throws Exception {
        long deckId = createDeck(alice, "Burn", "MODERN");
        mvc.perform(auth(patch("/api/decks/" + deckId), alice).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Legacy Burn\", \"format\": \"LEGACY\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Legacy Burn"))
            .andExpect(jsonPath("$.validation.format").value("LEGACY"));

        mvc.perform(auth(delete("/api/decks/" + deckId), alice)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/decks/" + deckId), alice)).andExpect(status().isNotFound());
    }

    @Test
    void importTextList_thenExport() throws Exception {
        String list = """
            Commander
            1 Testcard Dragon

            Deck
            1 Testcard Bolt (TPR) 2
            1 Testcard Delver
            1 Card That Does Not Exist
            """;
        String body = mvc.perform(auth(post("/api/decks/import"), alice).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Imported\", \"format\": \"COMMANDER\", \"list\": " + jsonString(list) + "}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.deck.cards", hasSize(3)))
            .andExpect(jsonPath("$.unresolvedCards", containsInAnyOrder("Card That Does Not Exist")))
            // "(TPR) 2" picked that exact printing instead of the default one
            .andExpect(jsonPath("$.deck.cards[?(@.name == 'Testcard Bolt')].setCode").value("tpr"))
            // the front-face name found the double-faced card
            .andExpect(jsonPath("$.deck.cards[?(@.name == 'Testcard Delver // Testcard Aberration')]").exists())
            .andReturn().getResponse().getContentAsString();
        Number deckId = JsonPath.read(body, "$.deck.id");

        mvc.perform(auth(get("/api/decks/" + deckId + "/export"), alice))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Commander\n1 Testcard Dragon (TST) 10\n")))
            .andExpect(content().string(containsString("1 Testcard Bolt (TPR) 2\n")));
    }

    @Test
    void badInput_isRejected() throws Exception {
        long deckId = createDeck(alice, "Burn", "MODERN");
        // printing that does not exist
        mvc.perform(auth(put("/api/decks/" + deckId + "/cards"), alice).contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(UUID.randomUUID().toString(), "MAIN", 1))).andExpect(status().isNotFound());
        // quantity over the limit
        mvc.perform(auth(put("/api/decks/" + deckId + "/cards"), alice).contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(BOLT, "MAIN", 251))).andExpect(status().isBadRequest());
        // unknown format
        mvc.perform(auth(post("/api/decks"), alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\": \"X\", \"format\": \"NOT_A_FORMAT\"}")).andExpect(status().isBadRequest());
        // no token at all
        mvc.perform(get("/api/decks")).andExpect(status().isUnauthorized());
    }

    // ---------- helpers ----------

    private static String printing(int n) {
        return String.format("00000000-0000-4000-8000-%012d", n);
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String registerAndGetToken() throws Exception {
        String email = "deck-test-" + UUID.randomUUID() + "@example.com";
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\": \"Tester\", \"email\": \"" + email
                    + "\", \"password\": \"Correct-Horse-Battery-42\", \"acceptTerms\": true}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private long createDeck(String token, String name, String format) throws Exception {
        String body = mvc.perform(auth(post("/api/decks"), token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"" + name + "\", \"format\": \"" + format + "\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void setCard(String token, long deckId, String scryfallId, String board, int quantity) throws Exception {
        mvc.perform(auth(put("/api/decks/" + deckId + "/cards"), token).contentType(MediaType.APPLICATION_JSON)
                .content(cardJson(scryfallId, board, quantity)))
            .andExpect(status().isOk());
    }

    private static String cardJson(String scryfallId, String board, int quantity) {
        return "{\"scryfallId\": \"" + scryfallId + "\", \"board\": \"" + board + "\", \"quantity\": " + quantity + "}";
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
