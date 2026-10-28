package com.nutrition.msnutritionplanner.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApiInfo() {
        return new OpenAPI().info(new Info()
                .title("ms-nutrition-planner API")
                .description("Contexto delimitado: Planes Nutricionales y Recetas — Nur-tricenter")
                .version("v1")
                .contact(new Contact().name("Nur-tricenter")));
    }
}
