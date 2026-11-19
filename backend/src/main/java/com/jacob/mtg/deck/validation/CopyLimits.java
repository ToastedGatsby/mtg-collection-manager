package com.jacob.mtg.deck.validation;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How many copies of a card a deck may contain. Besides the format's normal limit (4, or 1 in
 * Commander), some cards override it in their own rules text, which we read instead of hard-coding
 * card names, so new cards like this work automatically:
 *  - basic lands: any number;
 *  - "A deck can have any number of cards named Relentless Rats." -> any number;
 *  - "A deck can have up to seven cards named Seven Dwarves." -> 7 (Nazgûl: nine).
 */
public final class CopyLimits {

    public static final int UNLIMITED = Integer.MAX_VALUE;

    private static final Pattern ANY_NUMBER =
        Pattern.compile("a deck can have any number of cards named", Pattern.CASE_INSENSITIVE);
    private static final Pattern UP_TO =
        Pattern.compile("a deck can have up to (\\w+) cards named", Pattern.CASE_INSENSITIVE);
    private static final Map<String, Integer> NUMBER_WORDS = Map.ofEntries(
        Map.entry("two", 2), Map.entry("three", 3), Map.entry("four", 4), Map.entry("five", 5),
        Map.entry("six", 6), Map.entry("seven", 7), Map.entry("eight", 8), Map.entry("nine", 9),
        Map.entry("ten", 10), Map.entry("eleven", 11), Map.entry("twelve", 12), Map.entry("fifteen", 15),
        Map.entry("twenty", 20));

    private CopyLimits() {
    }

    public static int maxCopies(ValidationCard card, int formatLimit) {
        if (isBasicLand(card) || ANY_NUMBER.matcher(card.oracleText()).find()) {
            return UNLIMITED;
        }
        Matcher upTo = UP_TO.matcher(card.oracleText());
        if (upTo.find()) {
            Integer n = parseNumber(upTo.group(1));
            if (n != null) {
                return n;
            }
        }
        return formatLimit;
    }

    public static boolean isBasicLand(ValidationCard card) {
        return card.frontTypeContains("basic") && card.frontTypeContains("land");
    }

    private static Integer parseNumber(String word) {
        String w = word.toLowerCase(Locale.ROOT);
        if (w.chars().allMatch(Character::isDigit)) {
            return Integer.parseInt(w);
        }
        return NUMBER_WORDS.get(w);
    }
}
