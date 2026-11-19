package com.jacob.mtg.card.scryfall;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson 3 mapper for Scryfall JSON. Scryfall uses snake_case ("oracle_id"), Java uses camelCase
 * ("oracleId"); the naming strategy translates automatically. Unknown fields are ignored because a card
 * object has ~80 fields and we only keep what we need (and Scryfall may add new ones at any time).
 */
public final class ScryfallJson {

    public static final JsonMapper MAPPER = JsonMapper.builder()
        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();

    private ScryfallJson() {
    }
}
