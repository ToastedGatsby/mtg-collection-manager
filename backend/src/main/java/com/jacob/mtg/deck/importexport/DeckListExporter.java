package com.jacob.mtg.deck.importexport;

import java.util.List;
import java.util.Locale;

import com.jacob.mtg.deck.Board;

/** Writes a deck in MTG Arena's text format (also accepted by Moxfield, Archidekt, MTGO import). */
public final class DeckListExporter {

    public record ExportLine(Board board, int quantity, String name, String setCode, String collectorNumber) {
    }

    private static final List<Board> ORDER = List.of(Board.COMMANDER, Board.MAIN, Board.SIDEBOARD, Board.MAYBEBOARD);

    private DeckListExporter() {
    }

    public static String export(List<ExportLine> lines) {
        StringBuilder out = new StringBuilder();
        for (Board board : ORDER) {
            List<ExportLine> section = lines.stream().filter(l -> l.board() == board).toList();
            if (section.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append('\n');
            }
            out.append(header(board)).append('\n');
            for (ExportLine line : section) {
                out.append(line.quantity()).append(' ').append(line.name());
                if (line.setCode() != null) {
                    out.append(" (").append(line.setCode().toUpperCase(Locale.ROOT)).append(')');
                    if (line.collectorNumber() != null && !line.collectorNumber().isBlank()) {
                        out.append(' ').append(line.collectorNumber());
                    }
                }
                out.append('\n');
            }
        }
        return out.toString();
    }

    private static String header(Board board) {
        return switch (board) {
            case COMMANDER -> "Commander";
            case MAIN -> "Deck";
            case SIDEBOARD -> "Sideboard";
            case MAYBEBOARD -> "Maybeboard";
        };
    }
}
