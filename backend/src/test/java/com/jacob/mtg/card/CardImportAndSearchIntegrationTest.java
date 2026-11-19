package com.jacob.mtg.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.jacob.mtg.card.importer.CardImportService;
import com.jacob.mtg.card.importer.CardImportService.ImportResult;

/**
 * Imports a small Scryfall-shaped fixture (src/test/resources/scryfall/sample-cards.json) and checks the
 * search API against it. Every fixture card is named "Testcard ..." and every query filters on that name,
 * so the tests also pass on a dev database that already holds the real 100k cards. Rolled back after each test.
 */
@SpringBootTest
@Transactional
class CardImportAndSearchIntegrationTest {

    private static final String BOLT_ORACLE_ID = "00000000-0000-4000-9000-000000000001";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CardImportService importService;

    private MockMvc mvc;

    @BeforeEach
    void importFixture() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        try (InputStream json = getClass().getResourceAsStream("/scryfall/sample-cards.json")) {
            ImportResult result = importService.importFrom(json);
            assertThat(result.imported()).isEqualTo(9); // 11 objects - 1 token - 1 digital-only card
            assertThat(result.skipped()).isEqualTo(2);
        }
    }

    @Test
    void searchByName_returnsOneRowPerCard_withNonPromoDefaultPrinting() throws Exception {
        mvc.perform(get("/api/cards").param("q", "testcard bolt"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Testcard Bolt"))
            .andExpect(jsonPath("$.content[0].setCode").value("tst")) // the promo printing is not the default
            .andExpect(jsonPath("$.content[0].prices.eur").value(1.20))
            .andExpect(jsonPath("$.content[0].images.normal").isNotEmpty());
    }

    @Test
    void tokensAndDigitalOnlyCards_areNotImported() throws Exception {
        mvc.perform(get("/api/cards").param("q", "testcard goblin"))
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/cards").param("q", "testcard digital"))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void doubleFacedCard_takesManaCostTextAndImageFromItsFrontFace() throws Exception {
        mvc.perform(get("/api/cards").param("q", "testcard delver"))
            .andExpect(jsonPath("$.content[0].manaCost").value("{U}"))
            .andExpect(jsonPath("$.content[0].colors", contains("U")))
            .andExpect(jsonPath("$.content[0].images.normal").isNotEmpty());
    }

    @Test
    void colorFilters_includingExactAndCommanderIdentity() throws Exception {
        mvc.perform(get("/api/cards").param("q", "testcard").param("colors", "R"))
            .andExpect(jsonPath("$.content[*].name",
                containsInAnyOrder("Testcard Bolt", "Testcard Dragon", "Testcard Fire // Testcard Ice")));

        mvc.perform(get("/api/cards").param("q", "testcard").param("colors", "R").param("colorMode", "exact"))
            .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Testcard Bolt", "Testcard Dragon")));

        // a blue-red commander can play red, blue, blue-red and colourless cards - not green
        mvc.perform(get("/api/cards").param("q", "testcard").param("colors", "UR").param("colorMode", "identity"))
            .andExpect(jsonPath("$.totalElements").value(6));

        mvc.perform(get("/api/cards").param("q", "testcard").param("colors", "C"))
            .andExpect(jsonPath("$.content[*].name", contains("Testcard Ring")));
    }

    @Test
    void typeCmcRarityAndFormatFilters() throws Exception {
        mvc.perform(get("/api/cards").param("q", "testcard").param("type", "creature"))
            .andExpect(jsonPath("$.totalElements").value(3));

        mvc.perform(get("/api/cards").param("q", "testcard").param("cmcMin", "2").param("cmcMax", "6"))
            .andExpect(jsonPath("$.totalElements").value(3));

        mvc.perform(get("/api/cards").param("q", "testcard").param("rarity", "mythic"))
            .andExpect(jsonPath("$.content[*].name", contains("Testcard Dragon")));

        mvc.perform(get("/api/cards").param("q", "testcard").param("format", "standard"))
            .andExpect(jsonPath("$.content[*].name", contains("Testcard Elves")));

        // "restricted" counts as playable (1 copy) in Vintage
        mvc.perform(get("/api/cards").param("q", "testcard").param("format", "vintage"))
            .andExpect(jsonPath("$.totalElements").value(7));
    }

    @Test
    void sortingAndPaging() throws Exception {
        mvc.perform(get("/api/cards").param("q", "testcard").param("sort", "price").param("size", "2"))
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].name").value("Testcard Dragon"))
            .andExpect(jsonPath("$.totalElements").value(7))
            .andExpect(jsonPath("$.totalPages").value(4));
    }

    @Test
    void userInputIsTreatedAsData_notSql() throws Exception {
        mvc.perform(get("/api/cards").param("q", "' OR 1=1 --"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
        // unknown format / sort values are ignored, not injected
        mvc.perform(get("/api/cards").param("q", "testcard bolt").param("format", "modern; DROP TABLE users")
                .param("sort", "name; DROP TABLE users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void detailAndPrintings() throws Exception {
        mvc.perform(get("/api/cards/" + BOLT_ORACLE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.oracleText").value("Testcard Bolt deals 3 damage to any target."))
            .andExpect(jsonPath("$.legalities.modern").value("legal"));

        mvc.perform(get("/api/cards/" + BOLT_ORACLE_ID + "/printings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].setCode").value("tpr")) // newest first
            .andExpect(jsonPath("$[1].prices.usd").value(1.50))
            .andExpect(jsonPath("$[1].tcgplayerUrl").isNotEmpty());
    }

    @Test
    void badIdsReturn400Or404_notServerErrors() throws Exception {
        mvc.perform(get("/api/cards/not-a-uuid")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/cards/00000000-0000-4000-9000-999999999999")).andExpect(status().isNotFound());
    }

    @Test
    void reimportingTheSameData_updatesInsteadOfDuplicating() throws Exception {
        try (InputStream json = getClass().getResourceAsStream("/scryfall/sample-cards.json")) {
            importService.importFrom(json);
        }
        mvc.perform(get("/api/cards/" + BOLT_ORACLE_ID + "/printings"))
            .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void startingAnImport_requiresAdmin() throws Exception {
        mvc.perform(post("/api/admin/cards/import")).andExpect(status().isUnauthorized());
    }
}
