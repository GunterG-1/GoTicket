package com.GoTicket.ApiManager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxyAuthorizationServiceTest {
    private static final String USER_EMAIL = "cliente@example.com";
    private ProxyAuthorizationService authorization;
    private UsernamePasswordAuthenticationToken user;
    private UsernamePasswordAuthenticationToken admin;

    @BeforeEach
    void setUp() {
        authorization = new ProxyAuthorizationService(new ObjectMapper());
        user = new UsernamePasswordAuthenticationToken(USER_EMAIL, null,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
        admin = new UsernamePasswordAuthenticationToken("admin@duocuc.cl", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void permiteCatalogoPublicoSinSesion() {
        assertTrue(authorization.isAllowed(HttpMethod.GET, "/eventos/12", new byte[0], null));
        assertTrue(authorization.isAllowed(HttpMethod.GET, "/recintos", new byte[0], null));
    }

    @Test
    void limitaLecturaDeOrdenesPagosYEntradasAlPropietario() {
        String ownId = ProxyAuthorizationService.deriveUserId(USER_EMAIL);

        assertTrue(authorization.isAllowed(HttpMethod.GET, "/ordenes/usuario/" + ownId, new byte[0], user));
        assertTrue(authorization.isAllowed(HttpMethod.GET, "/pagos/usuario/" + ownId, new byte[0], user));
        assertTrue(authorization.isAllowed(HttpMethod.GET, "/inventarios/usuario/" + ownId + "/entradas",
                new byte[0], user));
        assertTrue(authorization.isAllowed(HttpMethod.GET,
                "/inventarios/usuario/" + ownId + "/entradas/7/pdf", new byte[0], user));
        assertFalse(authorization.isAllowed(HttpMethod.GET,
                "/inventarios/usuario/123/entradas/7/pdf", new byte[0], user));
        assertFalse(authorization.isAllowed(HttpMethod.GET, "/ordenes/usuario/123", new byte[0], user));
        assertFalse(authorization.isAllowed(HttpMethod.GET, "/ordenes/42", new byte[0], user));
        assertFalse(authorization.isAllowed(HttpMethod.GET, "/pagos/42", new byte[0], user));
        assertFalse(authorization.isAllowed(HttpMethod.GET, "/inventarios/evento/12/entradas",
                new byte[0], user));
    }

    @Test
    void autorizaConElIdPersistenteDelPrincipalJwt() {
        var persistedUser = new UsernamePasswordAuthenticationToken("987654321", null,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
        byte[] checkout = json("{\"idUsuario\":987654321}");

        assertTrue(authorization.isAllowed(HttpMethod.GET, "/ordenes/usuario/987654321",
                new byte[0], persistedUser));
        assertTrue(authorization.isAllowed(HttpMethod.POST, "/ordenes/checkout", checkout, persistedUser));
        assertFalse(authorization.isAllowed(HttpMethod.POST, "/ordenes/checkout",
                json("{\"idUsuario\":123}"), persistedUser));
    }

    @Test
    void obligaAQueCheckoutYReventaCorrespondanAlUsuarioAutenticado() {
        String ownId = ProxyAuthorizationService.deriveUserId(USER_EMAIL);
        byte[] checkout = json("{\"idUsuario\":\"" + ownId + "\"}");
        byte[] otherUserCheckout = json("{\"idUsuario\":\"123\"}");
        byte[] resaleReservation = json("{\"idUsuarioDestino\":\"" + ownId + "\"}");

        assertTrue(authorization.isAllowed(HttpMethod.POST, "/ordenes/checkout", checkout, user));
        assertFalse(authorization.isAllowed(HttpMethod.POST, "/ordenes/checkout", otherUserCheckout, user));
        assertTrue(authorization.isAllowed(HttpMethod.POST,
                "/inventarios/entradas/5/reventa/reservar", resaleReservation, user));
        assertFalse(authorization.isAllowed(HttpMethod.POST, "/pagos", checkout, user));
        assertFalse(authorization.isAllowed(HttpMethod.PUT, "/ordenes/42", checkout, user));
    }

    @Test
    void soloAdminPuedeValidarQrYOmitirComprobacionesDePropiedad() {
        byte[] body = json("{\"codigoAcceso\":\"qr-token\"}");

        assertFalse(authorization.isAllowed(HttpMethod.POST, "/inventarios/acceso/validar", body, user));
        assertTrue(authorization.isAllowed(HttpMethod.POST, "/inventarios/acceso/validar", body, admin));
        assertTrue(authorization.isAllowed(HttpMethod.GET, "/ordenes/42", new byte[0], admin));
    }

    private byte[] json(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
