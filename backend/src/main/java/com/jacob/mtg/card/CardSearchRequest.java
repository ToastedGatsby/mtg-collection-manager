package com.jacob.mtg.card;

import java.math.BigDecimal;

/**
 * Query parameters of GET /api/cards, bound automatically by Spring from the URL
 * (e.g. /api/cards?q=bolt&colors=R&colorMode=identity&format=commander&page=0).
 * Every field is optional; values are validated/whitelisted in CardSearchRepository.
 *
 * @param q         name contains
 * @param text      rules text contains
 * @param colors    letters from WUBRGC, e.g. "UR"; C = colorless
 * @param colorMode including (default) | exact | identity (fits a commander's colour identity)
 * @param type      type line contains, e.g. "creature", "legendary", "dragon"
 * @param format    only cards legal (or restricted) in this format, e.g. "modern", "commander"
 * @param sort      name (default) | cmc | price
 */
public record CardSearchRequest(
    String q,
    String text,
    String colors,
    String colorMode,
    String type,
    BigDecimal cmcMin,
    BigDecimal cmcMax,
    String rarity,
    String set,
    String format,
    String sort,
    Integer page,
    Integer size) {
}
