package com.jacob.mtg.common.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/** Adds the "Authorize" button to Swagger UI: paste the accessToken from /api/auth/login there. */
@Configuration
@OpenAPIDefinition(info = @Info(title = "MTG Vault API", version = "v1",
    description = "Collection manager, deckbuilder and price tracker for Magic: The Gathering"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
