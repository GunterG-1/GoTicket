package com.GoTicket.ApiManager.config;

import com.GoTicket.ApiManager.model.ApiKey;
import com.GoTicket.ApiManager.model.ApiRoute;
import com.GoTicket.ApiManager.repository.ApiKeyRepository;
import com.GoTicket.ApiManager.repository.ApiRouteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ApiRouteRepository routeRepository;
    private final ApiKeyRepository apiKeyRepository;

    @Value("${catalogo.url}")
    private String catalogoUrl;

    @Value("${inventario.url}")
    private String inventarioUrl;

    @Value("${orden.url}")
    private String ordenUrl;

    @Value("${pagos.url}")
    private String pagosUrl;

    public DataInitializer(ApiRouteRepository routeRepository, ApiKeyRepository apiKeyRepository) {
        this.routeRepository = routeRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    public void run(String... args) {
        List<ApiRoute> backendRoutes = List.of(
            new ApiRoute("Catálogo de eventos", "eventos", catalogoUrl + "/api/eventos", true, 120, false, "Eventos publicados por el microservicio de catálogo"),
            new ApiRoute("Inventario de entradas", "inventarios", inventarioUrl + "/api/inventarios", true, 120, false, "Disponibilidad por evento"),
            new ApiRoute("Órdenes", "ordenes", ordenUrl + "/api/ordenes", true, 60, false, "Creación y consulta de órdenes"),
            new ApiRoute("Pagos", "pagos", pagosUrl + "/api/pagos", true, 60, false, "Registro y consulta de pagos")
        );

        backendRoutes.forEach(route -> routeRepository.findByPathPattern(route.getPathPattern()).orElseGet(() -> routeRepository.save(route)));

        if (apiKeyRepository.count() == 0) {
            // Claves API iniciales
            ApiKey key1 = new ApiKey(
                    "duoc-demo-key-2026",
                    "Alumno DUOC - Entorno Desarrollo",
                    true,
                    60
            );

            ApiKey key2 = new ApiKey(
                    "duoc-premium-key-9999",
                    "Empresa Partner - Producción",
                    true,
                    300
            );

            apiKeyRepository.save(key1);
            apiKeyRepository.save(key2);
        }
    }
}
