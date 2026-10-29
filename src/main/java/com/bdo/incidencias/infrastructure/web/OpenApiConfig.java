package com.bdo.incidencias.infrastructure.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI incidenciasOpenApi() {
        return new OpenAPI().info(new Info()
            .title("API de incidencias")
            .version("1.0.0")
            .description("Registro y consulta de incidencias técnicas de un equipo de soporte."));
    }
}
