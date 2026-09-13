package com.GoTicket.Inventario.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI inventarioOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("GoTicket Inventario API")
                .version("1.0.0")
                .description("Disponibilidad, reservas, bloqueo temporal y reventa de entradas."));
    }
}
