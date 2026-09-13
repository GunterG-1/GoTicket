package com.GoTicket.ApiManager.controller;

import com.GoTicket.ApiManager.model.ApiKey;
import com.GoTicket.ApiManager.repository.ApiKeyRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/keys")
@Tag(name = "Gestión de API Keys", description = "Endpoints para administrar llaves de acceso para clientes")
public class ApiKeyController {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyController(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @GetMapping
    @Operation(summary = "Listar todas las API Keys registradas")
    public List<ApiKey> getAllKeys() {
        return apiKeyRepository.findAll();
    }

    @PostMapping
    @Operation(summary = "Generar una nueva API Key para un cliente")
    public ResponseEntity<?> createKey(@Valid @RequestBody ApiKey key) {
        if (key.getKeyString() == null || key.getKeyString().trim().isEmpty()) {
            key.setKeyString("key_" + UUID.randomUUID().toString().substring(0, 16));
        }

        if (apiKeyRepository.existsByKeyString(key.getKeyString())) {
            return ResponseEntity.badRequest().body("La clave API ingresada ya existe.");
        }

        ApiKey saved = apiKeyRepository.save(key);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Activar o Desactivar una API Key")
    public ResponseEntity<?> toggleKey(@PathVariable Long id) {
        return apiKeyRepository.findById(id).map(key -> {
            key.setActive(!key.isActive());
            apiKeyRepository.save(key);
            return ResponseEntity.ok(key);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Revocar / Eliminar una API Key")
    public ResponseEntity<Void> deleteKey(@PathVariable Long id) {
        if (!apiKeyRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        apiKeyRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
