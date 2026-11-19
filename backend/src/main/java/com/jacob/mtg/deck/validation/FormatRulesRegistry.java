package com.jacob.mtg.deck.validation;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.jacob.mtg.deck.DeckFormat;

/** Picks the validation strategy for a format (simple factory over the Strategy implementations). */
@Component
public class FormatRulesRegistry {

    private final Map<DeckFormat, FormatRules> rules = new EnumMap<>(DeckFormat.class);

    public FormatRulesRegistry() {
        for (DeckFormat format : DeckFormat.values()) {
            rules.put(format, format == DeckFormat.COMMANDER ? new CommanderRules() : new ConstructedRules(format));
        }
    }

    public FormatRules forFormat(DeckFormat format) {
        return rules.get(format);
    }
}
