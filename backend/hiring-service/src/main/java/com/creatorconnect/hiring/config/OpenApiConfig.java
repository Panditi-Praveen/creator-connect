package com.creatorconnect.hiring.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration for the Hiring Service.
 *
 * <p>Adds service metadata to the generated documentation and registers a
 * {@code bearerAuth} security scheme so Swagger UI shows the lock icon on
 * every protected endpoint and lets users paste a JWT once to try the APIs.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Builds the OpenAPI document root with service info and the bearer scheme.
     *
     * @return the configured {@link OpenAPI} bean
     */
    @Bean
    public OpenAPI hiringServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CreatorConnect Hiring Service API")
                        .description("""
                                Hiring workflow — freelancers apply to projects and creators decide on the \
                                applications.

                                Every endpoint except the infrastructure routes (/actuator, /swagger-ui, \
                                /v3/api-docs) requires a JWT issued by the Auth Service. Paste the token \
                                via the Authorize button (bearerAuth). The freelancerId used by the APIs \
                                is taken directly from the token's userId claim; applications are scoped \
                                to that identity.
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
