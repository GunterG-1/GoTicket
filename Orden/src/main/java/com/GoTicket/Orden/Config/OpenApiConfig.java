package com.GoTicket.Orden.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI ordenOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("GoTicket Ordenes API")
                .version("1.0.0")
                .description("Creación y ciclo de vida de órdenes de compra."));
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
