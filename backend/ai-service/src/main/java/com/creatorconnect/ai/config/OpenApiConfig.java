package com.creatorconnect.ai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration for the AI Service.
 *
 * <p>Adds service metadata to the generated documentation and registers a
 * {@code bearerAuth} security scheme so Swagger UI shows the lock icon on the
 * protected discovery endpoint and lets users paste a JWT once to try it.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Builds the OpenAPI document root with service info and the bearer scheme.
     *
     * @return the configured {@link OpenAPI} bean
     */
    @Bean
    public OpenAPI aiServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CreatorConnect AI Service API")
                        .description("""
                                AI-assisted talent discovery.

                                POST /ai/discover accepts a natural-language hiring query, matches it
                                against the platform's freelancer profiles (fetched from the Profile
                                Service), and returns ranked results. It requires a JWT issued by the
                                Auth Service (bearerAuth). GET /ai/status is a public health check.
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("CreatorConnect Team")))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
