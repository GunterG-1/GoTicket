package com.GoTicket.ApiManager.controller;

import com.GoTicket.ApiManager.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints para autenticación del frontend con Google y emisión de JWT interno")
public class AuthController {

    private final JwtService jwtService;
    private final RestClient restClient = RestClient.create();

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/google")
    @Operation(summary = "Autenticar con Google desde el frontend", description = "Se espera recibir un token de Google para validarlo y responder con un JWT interno de GoTicket")
    public ResponseEntity<Map<String, String>> loginWithGoogle(@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                            @RequestBody(required = false) Map<String, Object> payload) {
        String token = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
        } else if (payload != null && payload.get("token") instanceof String s) {
            token = s;
        }

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Se requiere un token de autenticación válido."));
        }

        Map<?, ?> googleClaims;
        String receivedToken = token;
        try {
            googleClaims = restClient.get()
                    .uri(uriBuilder -> uriBuilder.scheme("https").host("oauth2.googleapis.com")
                    .path("/tokeninfo").queryParam("id_token", receivedToken).build())
                    .retrieve().body(Map.class);
        } catch (Exception exception) {
            return ResponseEntity.status(401).body(Map.of("error", "El token de Google no pudo validarse."));
        }
        if (googleClaims == null || !googleClientId.equals(googleClaims.get("aud"))) {
            return ResponseEntity.status(401).body(Map.of("error", "El token de Google no pertenece a GoTicket."));
        }
        Object emailClaim = googleClaims.get("email");
        String email = emailClaim == null ? "" : String.valueOf(emailClaim);
        Object nameClaim = googleClaims.get("name");
        String name = nameClaim == null ? email : String.valueOf(nameClaim);
        if (email.isBlank() || !"true".equalsIgnoreCase(String.valueOf(googleClaims.get("email_verified")))) {
            return ResponseEntity.status(401).body(Map.of("error", "La cuenta de Google no está verificada."));
        }

        String role = email.trim().toLowerCase().endsWith("@duocuc.cl") ? "ADMIN" : "CLIENTE";
        String jwt = jwtService.generateToken(email, name, role);
        String userId = deriveUserId(email);

        return ResponseEntity.ok(Map.of(
                "token", jwt,
                "tokenType", "Bearer",
                "idUsuario", userId,
                "email", email,
                "name", name,
                "role", role
        ));
    }

    private String deriveUserId(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        return String.valueOf(Math.abs(email.trim().toLowerCase().hashCode()));
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener información del usuario autenticado")
    public ResponseEntity<Map<String, String>> me() {
        return ResponseEntity.ok(Map.of(
                "message", "Autenticación activa",
                "status", "OK"
        ));
    }
}
