package com.GoTicket.Pagos.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI pagosOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("GoTicket Pagos API")
                .version("1.0.0")
                .description("Registro de pagos y publicación de resultados transaccionales."));
    }
}
