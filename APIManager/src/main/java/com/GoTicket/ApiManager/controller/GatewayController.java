package com.GoTicket.ApiManager.controller;

import com.GoTicket.ApiManager.model.ApiKey;
import com.GoTicket.ApiManager.model.ApiRoute;
import com.GoTicket.ApiManager.repository.ApiKeyRepository;
import com.GoTicket.ApiManager.repository.ApiRouteRepository;
import com.GoTicket.ApiManager.service.ProxyService;
import com.GoTicket.ApiManager.service.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.Optional;

@RestController
@RequestMapping("/api/proxy")
@Tag(name = "Gateway", description = "Punto de entrada Proxy para encaminar tráfico a los servicios backend registrados")
public class GatewayController {

    private final ApiRouteRepository routeRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final RateLimiterService rateLimiterService;
    private final ProxyService proxyService;

    public GatewayController(ApiRouteRepository routeRepository,
                             ApiKeyRepository apiKeyRepository,
                             RateLimiterService rateLimiterService,
                             ProxyService proxyService) {
        this.routeRepository = routeRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.rateLimiterService = rateLimiterService;
        this.proxyService = proxyService;
    }

    @RequestMapping(value = "/{pattern}/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    @Operation(summary = "Reenviar petición a la API configurada", description = "Evalúa la ruta, valida la API Key (header X-API-KEY) y la cuota de peticiones antes de despachar al servidor de destino.")
    public ResponseEntity<byte[]> handleProxy(
            @PathVariable("pattern") String pattern,
            HttpServletRequest request,
            Authentication authentication) {

        Optional<ApiRoute> routeOpt = routeRepository.findByPathPattern(pattern);
        if (routeOpt.isEmpty()) {
            return errorResponse(HttpStatus.NOT_FOUND, "No existe una API registrada con el patrón de ruta: '" + pattern + "'");
        }

        ApiRoute route = routeOpt.get();
        if (!route.isEnabled()) {
            return errorResponse(HttpStatus.SERVICE_UNAVAILABLE, "La API '" + route.getName() + "' se encuentra temporalmente deshabilitada.");
        }

        if (!isAllowedUserResource(request, authentication)) {
            return errorResponse(HttpStatus.FORBIDDEN, "No puedes consultar recursos de otro usuario");
        }

        String apiKeyHeader = request.getHeader("X-API-KEY");
        String apiKeyUsed = apiKeyHeader != null ? apiKeyHeader : "ANONYMOUS";

        if (route.isRequiresApiKey()) {
            if (apiKeyHeader == null || apiKeyHeader.trim().isEmpty()) {
                return errorResponse(HttpStatus.UNAUTHORIZED, "Esta API requiere una clave de acceso (Header 'X-API-KEY')");
            }

            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyString(apiKeyHeader);
            if (apiKeyOpt.isEmpty() || !apiKeyOpt.get().isActive()) {
                return errorResponse(HttpStatus.FORBIDDEN, "La clave API proporcionada es inválida o ha sido desactivada");
            }
        }

        // Rate Limiting
        int limit = route.getRateLimitPerMinute();
        String limitKey = "route:" + route.getId() + ":" + (apiKeyHeader != null ? apiKeyHeader : request.getRemoteAddr());
        if (!rateLimiterService.isAllowed(limitKey, limit)) {
            return errorResponse(HttpStatus.TOO_MANY_REQUESTS, "Límite de peticiones excedido para esta API (" + limit + " peticiones/minuto)");
        }

        // Extraer sub-ruta residual
        String requestUri = request.getRequestURI();
        String prefix = "/api/proxy/" + pattern;
        String remainingPath = "";
        if (requestUri.length() > prefix.length()) {
            remainingPath = requestUri.substring(prefix.length());
        }

        return proxyService.forwardRequest(route, remainingPath, request, apiKeyUsed);
    }

    private boolean isAllowedUserResource(HttpServletRequest request, Authentication authentication) {
        String uri = request.getRequestURI();
        if (authentication == null || authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            return true;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("/usuario/(\\d+)").matcher(uri);
        if (!matcher.find()) return true;
        String email = authentication.getName();
        String expectedId = String.valueOf(Math.abs(email.trim().toLowerCase().hashCode()));
        return expectedId.equals(matcher.group(1));
    }

    private ResponseEntity<byte[]> errorResponse(HttpStatus status, String message) {
        String json = "{\"status\": " + status.value() + ", \"error\": \"" + message + "\"}";
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.getBytes());
    }
}
