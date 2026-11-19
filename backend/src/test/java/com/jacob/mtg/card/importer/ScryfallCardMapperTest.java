package com.jacob.mtg.card.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.jacob.mtg.card.importer.CardRows.ImportRow;
import com.jacob.mtg.card.scryfall.ScryfallCard;
import com.jacob.mtg.card.scryfall.ScryfallCard.Face;
import com.jacob.mtg.card.scryfall.ScryfallCard.Prices;

/** Plain unit tests (no Spring, no database): run in milliseconds. */
class ScryfallCardMapperTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID ORACLE = UUID.randomUUID();

    @Test
    void mapsANormalCard() {
        ImportRow row = ScryfallCardMapper.map(card("normal", "{R}", "Deal 3 damage.", List.of("R"),
            Map.of("normal", "img"), null, new Prices("1.50", null, null, "1.20", null, null), false)).orElseThrow();

        assertThat(row.oracle().manaCost()).isEqualTo("{R}");
        assertThat(row.oracle().colors()).containsExactly("R");
        assertThat(row.printing().priceUsd()).isEqualByComparingTo("1.50");
        assertThat(row.printing().priceEur()).isEqualByComparingTo(new BigDecimal("1.20"));
        assertThat(row.printing().imageUrisJson()).contains("\"normal\"");
    }

    @Test
    void doubleFacedCard_fallsBackToTheFrontFace() {
        List<Face> faces = List.of(
            new Face("Front", null, "{U}", "Creature", "Front text", List.of("U"), Map.of("normal", "front.jpg")),
            new Face("Back", null, "", "Creature", "Back text", List.of("U"), Map.of("normal", "back.jpg")));

        ImportRow row = ScryfallCardMapper.map(card("transform", null, null, null, null, faces, null, false)).orElseThrow();

        assertThat(row.oracle().manaCost()).isEqualTo("{U}"); // empty back-face cost is ignored
        assertThat(row.oracle().oracleText()).isEqualTo("Front text\n//\nBack text");
        assertThat(row.oracle().colors()).containsExactly("U");
        assertThat(row.printing().imageUrisJson()).contains("front.jpg");
    }

    @Test
    void skipsTokensAndDigitalOnlyCards() {
        assertThat(ScryfallCardMapper.map(card("token", "", null, List.of(), null, null, null, false))).isEmpty();
        assertThat(ScryfallCardMapper.map(card("normal", "{B}", null, List.of("B"), null, null, null, true))).isEmpty();
    }

    @Test
    void malformedPrice_becomesNull_insteadOfFailingTheImport() {
        assertThat(ScryfallCardMapper.price("not-a-number")).isNull();
        assertThat(ScryfallCardMapper.price(null)).isNull();
        assertThat(ScryfallCardMapper.price("0.05")).isEqualByComparingTo("0.05");
    }

    private static ScryfallCard card(String layout, String manaCost, String text, List<String> colors,
                                     Map<String, String> images, List<Face> faces, Prices prices, boolean digital) {
        return new ScryfallCard(ID, ORACLE, "Test", layout, manaCost, 1.0, "Instant", text, colors, List.of(),
            List.of(), Map.of("modern", "legal"), "tst", "Test Set", "1", "common", "en", List.of("nonfoil"),
            images, faces, 1, 2, "2020-01-01", prices, Map.of(), digital, false, false, "black");
    }
}
