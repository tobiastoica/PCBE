package com.fleet.central.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI centralServerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Urban Micro-Mobility Central Server & Database API")
                        .description("Authoritative Central Management Server, SSoT Database, Redis Geospatial Cache, and Kafka Event Orchestration API.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth").addList("ApiKeyAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .name("BearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT token obtained from /api/v1/auth/login"))
                        .addSecuritySchemes("ApiKeyAuth", new SecurityScheme()
                                .name("X-API-KEY")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Enter internal service API Key (default: fleet-secret-key-2026)")));
    }
}
