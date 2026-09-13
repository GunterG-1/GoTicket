package com.GoTicket.ApiManager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mock")
@Tag(name = "Servicios Mock Internos", description = "Endpoints de simulación backend para probar el enrutamiento del API Gateway de inmediato")
public class MockBackendController {

    @GetMapping("/users")
    @Operation(summary = "Servicio simulado de usuarios")
    public List<Map<String, Object>> getMockUsers() {
        List<Map<String, Object>> users = new ArrayList<>();

        Map<String, Object> u1 = new HashMap<>();
        u1.put("id", 101);
        u1.put("nombre", "Javier Pérez");
        u1.put("email", "javier@duoc.cl");
        u1.put("carrera", "Ingeniería en Informática");

        Map<String, Object> u2 = new HashMap<>();
        u2.put("id", 102);
        u2.put("nombre", "María González");
        u2.put("email", "maria@duoc.cl");
        u2.put("carrera", "Desarrollo de Software");

        users.add(u1);
        users.add(u2);
        return users;
    }

    @GetMapping("/products")
    @Operation(summary = "Servicio simulado de catálogo de productos")
    public List<Map<String, Object>> getMockProducts() {
        List<Map<String, Object>> products = new ArrayList<>();

        Map<String, Object> p1 = new HashMap<>();
        p1.put("sku", "PROD-001");
        p1.put("nombre", "Servidor Cloud Spring Boot");
        p1.put("precio", 49.99);

        Map<String, Object> p2 = new HashMap<>();
        p2.put("sku", "PROD-002");
        p2.put("nombre", "Licencia API Gateway Pro");
        p2.put("precio", 129.00);

        products.add(p1);
        products.add(p2);
        return products;
    }

    @GetMapping("/status")
    @Operation(summary = "Servicio simulado de estado del sistema")
    public Map<String, Object> getMockStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("system", "DUOC Microservices Backend Mock");
        status.put("health", "UP");
        status.put("uptimeSeconds", 3600);
        status.put("timestamp", System.currentTimeMillis());
        return status;
    }
}
