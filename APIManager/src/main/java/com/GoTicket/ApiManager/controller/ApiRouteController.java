package com.GoTicket.ApiManager.controller;

import com.GoTicket.ApiManager.model.ApiRoute;
import com.GoTicket.ApiManager.repository.ApiRouteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routes")
@Tag(name = "Administración de Rutas", description = "CRUD para la gestión de las APIs registradas en el Manager")
public class ApiRouteController {

    private final ApiRouteRepository routeRepository;

    public ApiRouteController(ApiRouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @GetMapping
    @Operation(summary = "Listar todas las rutas registradas")
    public List<ApiRoute> getAllRoutes() {
        return routeRepository.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de una ruta por ID")
    public ResponseEntity<ApiRoute> getRouteById(@PathVariable Long id) {
        return routeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Registrar una nueva API en el Manager")
    public ResponseEntity<?> createRoute(@Valid @RequestBody ApiRoute route) {
        if (routeRepository.existsByPathPattern(route.getPathPattern())) {
            return ResponseEntity.badRequest().body("El patrón de ruta '" + route.getPathPattern() + "' ya se encuentra registrado.");
        }
        ApiRoute saved = routeRepository.save(route);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una API existente")
    public ResponseEntity<?> updateRoute(@PathVariable Long id, @Valid @RequestBody ApiRoute updated) {
        return routeRepository.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setPathPattern(updated.getPathPattern());
            existing.setTargetUrl(updated.getTargetUrl());
            existing.setEnabled(updated.isEnabled());
            existing.setRateLimitPerMinute(updated.getRateLimitPerMinute());
            existing.setRequiresApiKey(updated.isRequiresApiKey());
            existing.setDescription(updated.getDescription());
            ApiRoute saved = routeRepository.save(existing);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Habilitar/Deshabilitar el estado de una API")
    public ResponseEntity<?> toggleRoute(@PathVariable Long id) {
        return routeRepository.findById(id).map(existing -> {
            existing.setEnabled(!existing.isEnabled());
            routeRepository.save(existing);
            return ResponseEntity.ok(existing);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una ruta de API")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        if (!routeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        routeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
