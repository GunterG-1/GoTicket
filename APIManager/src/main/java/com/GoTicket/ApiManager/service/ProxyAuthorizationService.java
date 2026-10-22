package com.GoTicket.ApiManager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProxyAuthorizationService {
    private static final Pattern USER_RESOURCE = Pattern.compile(
            "^/(?:ordenes|pagos)/usuario/(-?\\d+)$|^/inventarios/usuario/(-?\\d+)/entradas$");
    private static final Pattern USER_TICKET_PDF = Pattern.compile(
            "^/inventarios/usuario/(-?\\d+)/entradas/\\d+/pdf$");
    private static final Pattern PUBLIC_INVENTORY_RESOURCE = Pattern.compile(
            "^/inventarios/(?:evento/\\d+(?:/sectores)?|reventa|entradas/\\d+/reventa)$");
    private static final Pattern RESALE_RESERVATION = Pattern.compile(
            "^/inventarios/entradas/\\d+/reventa/reservar$");
    private static final Pattern PAYMENT_SIMULATION = Pattern.compile("^/pagos/\\d+/simular$");

    private final ObjectMapper objectMapper;

    public ProxyAuthorizationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean isAllowed(HttpMethod method, String path, byte[] body, Authentication authentication) {
        if (HttpMethod.GET.equals(method) && path.matches("^/(?:eventos|recintos)(?:/.*)?$")) {
            return true;
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            return true;
        }

        if (HttpMethod.GET.equals(method)) {
            Matcher ticketPdf = USER_TICKET_PDF.matcher(path);
            if (ticketPdf.matches()) {
                return authenticatedUserId(authentication.getName()).equals(ticketPdf.group(1));
            }
            Matcher userResource = USER_RESOURCE.matcher(path);
            if (userResource.matches()) {
                String requestedUserId = userResource.group(1) != null
                        ? userResource.group(1) : userResource.group(2);
                return authenticatedUserId(authentication.getName()).equals(requestedUserId);
            }
            return PUBLIC_INVENTORY_RESOURCE.matcher(path).matches();
        }

        if (!HttpMethod.POST.equals(method)) {
            return false;
        }

        if (RESALE_RESERVATION.matcher(path).matches()) {
            return bodyMatchesUser(body, "idUsuarioDestino", authenticatedUserId(authentication.getName()));
        }
        if (PAYMENT_SIMULATION.matcher(path).matches()) {
            return bodyMatchesUser(body, "idUsuario", authenticatedUserId(authentication.getName()));
        }
        if ("/ordenes".equals(path) || "/ordenes/checkout".equals(path)
                || "/inventarios/reservas".equals(path)
                || "/inventarios/reservas/confirmar".equals(path)
                || path.matches("^/inventarios/entradas/\\d+/reventa$")) {
            return bodyMatchesUser(body, "idUsuario", authenticatedUserId(authentication.getName()));
        }
        return false;
    }

    public static String deriveUserId(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        return String.valueOf(Math.abs(email.trim().toLowerCase(Locale.ROOT).hashCode()));
    }

    private String authenticatedUserId(String principal) {
        try {
            return String.valueOf(Long.parseLong(principal));
        } catch (NumberFormatException exception) {
            return deriveUserId(principal);
        }
    }

    private boolean bodyMatchesUser(byte[] body, String field, String expectedUserId) {
        if (body == null || body.length == 0) {
            return false;
        }
        try {
            JsonNode userId = objectMapper.readTree(body).path(field);
            return !userId.isMissingNode() && !userId.isNull()
                    && expectedUserId.equals(userId.asText());
        } catch (IOException exception) {
            return false;
        }
    }
}
