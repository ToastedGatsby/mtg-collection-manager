package com.jacob.mtg.card.scryfall;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** app.scryfall.* from application.yml. */
@ConfigurationProperties("app.scryfall")
public record ScryfallProperties(
    String baseUrl,
    String userAgent,
    String bulkType,
    boolean importOnStartup,
    String importCron) {
}
