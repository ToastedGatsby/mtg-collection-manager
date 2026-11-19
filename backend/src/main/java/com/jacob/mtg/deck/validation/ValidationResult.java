package com.jacob.mtg.deck.validation;

import java.util.List;

import com.jacob.mtg.deck.DeckFormat;

/** Outcome of validating a deck: legal only if there are no issues. */
public record ValidationResult(
    DeckFormat format,
    boolean legal,
    int mainCount,
    int sideboardCount,
    int commanderCount,
    List<ValidationIssue> issues) {

    public static ValidationResult of(DeckFormat format, DeckCounts counts, List<ValidationIssue> issues) {
        return new ValidationResult(format, issues.isEmpty(), counts.main(), counts.sideboard(),
            counts.commander(), List.copyOf(issues));
    }
}
