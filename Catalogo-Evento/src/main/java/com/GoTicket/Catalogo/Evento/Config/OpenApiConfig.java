package com.GoTicket.Catalogo.Evento.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI catalogoOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("GoTicket Catalogo y Eventos API")
                .version("1.0.0")
                .description("Publicación y consulta del catálogo de eventos."));
    }
}
