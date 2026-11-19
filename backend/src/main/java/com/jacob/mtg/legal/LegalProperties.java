package com.jacob.mtg.legal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Current versions of the legal documents (app.legal.* in application.yml). */
@ConfigurationProperties("app.legal")
public record LegalProperties(String tosVersion, String privacyVersion) {
}
