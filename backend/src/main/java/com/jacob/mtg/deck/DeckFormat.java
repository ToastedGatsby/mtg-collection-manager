package com.jacob.mtg.deck;

import java.util.Locale;

/** Supported formats. legalityKey() is the key used in Scryfall's "legalities" object. */
public enum DeckFormat {
    STANDARD,
    PIONEER,
    MODERN,
    LEGACY,
    VINTAGE,
    PAUPER,
    COMMANDER;

    public String legalityKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
