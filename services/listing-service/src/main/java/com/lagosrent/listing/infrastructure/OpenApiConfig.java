package com.lagosrent.listing.infrastructure;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lagos Rent — Listing Service API")
                        .version("v1.0.0")
                        .description("REST API for Property, Rental Unit, and Listing lifecycle management")
                        .contact(new Contact().name("Lagos Rent Engineering").email("dev@lagosrent.ng")))
                .addSecurityItem(new SecurityRequirement().addList("Keycloak Bearer Auth"))
                .schemaRequirement("Keycloak Bearer Auth", new SecurityScheme()
                        .name("Keycloak Bearer Auth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                );
    }
}
