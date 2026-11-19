package com.jacob.mtg.deck.importexport;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.jacob.mtg.deck.Board;
import com.jacob.mtg.deck.importexport.DeckListExporter.ExportLine;
import com.jacob.mtg.deck.importexport.DeckListParser.ParseResult;
import com.jacob.mtg.deck.importexport.DeckListParser.ParsedLine;

class DeckListParserTest {

    @Test
    void arenaFormat_withSectionsAndPrintings() {
        ParseResult result = DeckListParser.parse("""
            Commander
            1 Krenko, Mob Boss (DDT) 52

            Deck
            4 Lightning Bolt (M10) 146
            20 Mountain

            Sideboard
            2 Abrade
            """);

        assertThat(result.errors()).isEmpty();
        assertThat(result.lines()).hasSize(4);
        ParsedLine krenko = result.lines().get(0);
        assertThat(krenko.board()).isEqualTo(Board.COMMANDER);
        assertThat(krenko.name()).isEqualTo("Krenko, Mob Boss");
        assertThat(krenko.setCode()).isEqualTo("ddt");
        assertThat(krenko.collectorNumber()).isEqualTo("52");
        assertThat(result.lines().get(1).quantity()).isEqualTo(4);
        assertThat(result.lines().get(2).setCode()).isNull();
        assertThat(result.lines().get(3).board()).isEqualTo(Board.SIDEBOARD);
    }

    @Test
    void mtgoFormat_blankLineStartsSideboard() {
        ParseResult result = DeckListParser.parse("4 Lightning Bolt\n4 Goblin Guide\n\n3 Smash to Smithereens\n");
        assertThat(result.lines().stream().map(ParsedLine::board).toList())
            .containsExactly(Board.MAIN, Board.MAIN, Board.SIDEBOARD);
    }

    @Test
    void quantityVariants_sbPrefix_foilMarker_andNoQuantity() {
        ParseResult result = DeckListParser.parse("""
            4x Lightning Bolt
            SB: 2 Abrade
            1 Sol Ring (C21) 263 *F*
            Counterspell
            """);
        assertThat(result.errors()).isEmpty();
        assertThat(result.lines().get(0).quantity()).isEqualTo(4);
        assertThat(result.lines().get(1).board()).isEqualTo(Board.SIDEBOARD);
        assertThat(result.lines().get(1).name()).isEqualTo("Abrade");
        assertThat(result.lines().get(2).name()).isEqualTo("Sol Ring");
        assertThat(result.lines().get(2).collectorNumber()).isEqualTo("263");
        assertThat(result.lines().get(3).quantity()).isEqualTo(1);
    }

    @Test
    void namesWithParenthesesAndDoubleFaces_stayIntact() {
        ParseResult result = DeckListParser.parse("1 B.F.M. (Big Furry Monster)\n1 Fire // Ice\n");
        assertThat(result.lines().get(0).name()).isEqualTo("B.F.M. (Big Furry Monster)");
        assertThat(result.lines().get(0).setCode()).isNull();
        assertThat(result.lines().get(1).name()).isEqualTo("Fire // Ice");
    }

    @Test
    void commentsAndArenaAboutBlock_areSkipped() {
        ParseResult result = DeckListParser.parse("About\nName Goblins\n// my notes\n# more notes\nDeck\n4 Goblin Guide\n");
        assertThat(result.lines()).hasSize(1);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void badQuantities_areReported_withLineNumbers() {
        ParseResult result = DeckListParser.parse("4 Lightning Bolt\n0 Shock\n999 Mountain\n");
        assertThat(result.lines()).hasSize(1);
        assertThat(result.errors()).hasSize(2);
        assertThat(result.errors().get(0).startsWith("Line 2")).isTrue();
    }

    @Test
    void emptyList() {
        assertThat(DeckListParser.parse("  \n ").errors()).containsExactly("The deck list is empty");
    }

    @Test
    void exportThenParse_roundTrips() {
        String text = DeckListExporter.export(List.of(
            new ExportLine(Board.MAIN, 4, "Lightning Bolt", "m10", "146"),
            new ExportLine(Board.COMMANDER, 1, "Krenko, Mob Boss", "ddt", "52"),
            new ExportLine(Board.SIDEBOARD, 2, "Abrade", null, null)));

        assertThat(text).isEqualTo("Commander\n1 Krenko, Mob Boss (DDT) 52\n\nDeck\n4 Lightning Bolt (M10) 146\n\nSideboard\n2 Abrade\n");
        ParseResult parsed = DeckListParser.parse(text);
        assertThat(parsed.lines().stream().map(ParsedLine::board).toList())
            .containsExactly(Board.COMMANDER, Board.MAIN, Board.SIDEBOARD);
    }
}
