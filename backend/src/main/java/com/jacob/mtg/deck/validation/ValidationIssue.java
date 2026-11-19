package com.jacob.mtg.deck.validation;

/**
 * One problem with a deck. The UI translates `code` (with `cardName`, `actual`, `limit`);
 * `message` is a readable English fallback for API clients.
 */
public record ValidationIssue(String code, String message, String cardName, Integer actual, Integer limit) {

    public static ValidationIssue deck(String code, String message, int actual, int limit) {
        return new ValidationIssue(code, message, null, actual, limit);
    }

    public static ValidationIssue card(String code, String message, String cardName) {
        return new ValidationIssue(code, message, cardName, null, null);
    }

    public static ValidationIssue card(String code, String message, String cardName, int actual, int limit) {
        return new ValidationIssue(code, message, cardName, actual, limit);
    }
}
