package com.jacob.mtg.deck.validation;

import java.util.List;

import com.jacob.mtg.deck.DeckFormat;

/**
 * Strategy pattern: one implementation per family of formats. The deck service does not know any
 * rule; it asks FormatRulesRegistry for the strategy of the deck's format and calls validate().
 * Adding a format = adding a class (or a registry entry), without touching existing code (Open/Closed).
 */
public interface FormatRules {

    DeckFormat format();

    ValidationResult validate(List<ValidationCard> cards);
}
