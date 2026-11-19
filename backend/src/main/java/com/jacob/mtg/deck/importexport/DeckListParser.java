package com.jacob.mtg.deck.importexport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jacob.mtg.deck.Board;

/**
 * Parses the text deck lists people copy from MTG Arena, MTGO, Moxfield, Archidekt...
 *
 * <pre>
 * Commander
 * 1 Krenko, Mob Boss (DDT) 52
 *
 * Deck
 * 4 Lightning Bolt (M10) 146
 * 4x Goblin Guide
 * 20 Mountain
 *
 * Sideboard
 * SB: 2 Abrade
 * </pre>
 *
 * Supports section headers (Deck/Sideboard/Commander/Companion/Maybeboard), "4x" and "4" quantities,
 * optional "(SET) number" printing, Moxfield's "*F*" foil marker, MTGO's "SB:" prefix, and the MTGO
 * convention that, without headers, a blank line separates main deck from sideboard.
 * Pure Java, no database: resolving names to cards happens later in DeckListImporter.
 */
public final class DeckListParser {

    public static final int MAX_LINES = 1000;
    public static final int MAX_QUANTITY = 250;

    /** "4x Lightning Bolt (M10) 146 *F*" -> qty, name, set, number. */
    private static final Pattern CARD_LINE = Pattern.compile(
        "^(?:(\\d+)\\s*[xX]?\\s+)?(.+?)(?:\\s+\\(([A-Za-z0-9]{2,8})\\)(?:\\s+([A-Za-z0-9★\\-]+))?)?(?:\\s+\\*[A-Z]+\\*)?$");

    private static final Map<String, Board> HEADERS = Map.ofEntries(
        Map.entry("deck", Board.MAIN), Map.entry("main", Board.MAIN), Map.entry("mainboard", Board.MAIN),
        Map.entry("main deck", Board.MAIN), Map.entry("sideboard", Board.SIDEBOARD), Map.entry("sb", Board.SIDEBOARD),
        Map.entry("companion", Board.SIDEBOARD), Map.entry("commander", Board.COMMANDER),
        Map.entry("commanders", Board.COMMANDER), Map.entry("maybeboard", Board.MAYBEBOARD),
        Map.entry("maybe", Board.MAYBEBOARD), Map.entry("considering", Board.MAYBEBOARD));

    public record ParsedLine(int lineNumber, int quantity, String name, String setCode, String collectorNumber, Board board) {
    }

    public record ParseResult(List<ParsedLine> lines, List<String> errors) {
    }

    private DeckListParser() {
    }

    public static ParseResult parse(String text) {
        List<ParsedLine> lines = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return new ParseResult(lines, List.of("The deck list is empty"));
        }

        String[] rawLines = text.split("\\R");
        if (rawLines.length > MAX_LINES) {
            return new ParseResult(lines, List.of("The deck list has more than " + MAX_LINES + " lines"));
        }
        boolean usesHeaders = false;
        boolean skipNextName = false;
        Board board = Board.MAIN;

        for (int i = 0; i < rawLines.length; i++) {
            int lineNumber = i + 1;
            String line = rawLines[i].strip();

            if (line.isEmpty()) {
                // MTGO: without headers, the first blank line after the main deck starts the sideboard
                if (!usesHeaders && board == Board.MAIN && !lines.isEmpty()) {
                    board = Board.SIDEBOARD;
                }
                continue;
            }
            if (line.startsWith("//") || line.startsWith("#")) {
                continue;
            }
            if (skipNextName || line.equalsIgnoreCase("about")) {
                // Arena "About" block: "About" / "Name My Deck"
                skipNextName = line.equalsIgnoreCase("about");
                continue;
            }
            Board header = HEADERS.get(line.replaceAll(":$", "").strip().toLowerCase(Locale.ROOT));
            if (header != null) {
                board = header;
                usesHeaders = true;
                continue;
            }

            Board lineBoard = board;
            if (line.regionMatches(true, 0, "SB:", 0, 3)) {
                lineBoard = Board.SIDEBOARD;
                line = line.substring(3).strip();
            }

            Matcher m = CARD_LINE.matcher(line);
            if (!m.matches()) {
                errors.add("Line " + lineNumber + ": cannot read \"" + truncate(line) + "\"");
                continue;
            }
            int quantity;
            try {
                quantity = m.group(1) == null ? 1 : Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                quantity = -1;
            }
            if (quantity < 1 || quantity > MAX_QUANTITY) {
                errors.add("Line " + lineNumber + ": quantity must be between 1 and " + MAX_QUANTITY);
                continue;
            }
            lines.add(new ParsedLine(lineNumber, quantity, m.group(2).strip(),
                m.group(3) == null ? null : m.group(3).toLowerCase(Locale.ROOT), m.group(4), lineBoard));
        }
        if (lines.isEmpty() && errors.isEmpty()) {
            errors.add("No cards found in the deck list");
        }
        return new ParseResult(lines, errors);
    }

    private static String truncate(String s) {
        return s.length() <= 60 ? s : s.substring(0, 60) + "…";
    }
}
