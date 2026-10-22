package com.GoTicket.ApiManager.service;

import com.GoTicket.ApiManager.model.ActualizarPerfilRequest;
import com.GoTicket.ApiManager.model.IdentidadGoogleRequest;
import com.GoTicket.ApiManager.model.UsuarioPerfilResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UsuarioClient {
    private final RestClient restClient = RestClient.create();

    @Value("${catalogo.url:http://localhost:8081}")
    private String catalogoUrl;

    @Value("${goticket.internal.api-token:}")
    private String internalApiToken;

    public UsuarioPerfilResponse sincronizarIdentidadGoogle(Long idUsuario, String googleSubject,
            String email, String nombre) {
        UsuarioPerfilResponse profile = restClient.post()
                .uri(catalogoUrl + "/api/usuarios/google")
                .header("X-GoTicket-Internal-Token", internalApiToken)
                .body(new IdentidadGoogleRequest(idUsuario, googleSubject, email, nombre))
                .retrieve()
                .body(UsuarioPerfilResponse.class);
        if (profile == null) {
            throw new IllegalStateException("Catalogo-Evento no devolvió el perfil autenticado");
        }
        return profile;
    }

    public UsuarioPerfilResponse obtenerPerfil(Long idUsuario) {
        UsuarioPerfilResponse profile = restClient.get()
                .uri(catalogoUrl + "/api/usuarios/{id}", idUsuario)
                .header("X-GoTicket-Internal-Token", internalApiToken)
                .retrieve()
                .body(UsuarioPerfilResponse.class);
        if (profile == null) {
            throw new IllegalStateException("Catalogo-Evento no devolvió el perfil solicitado");
        }
        return profile;
    }

    public UsuarioPerfilResponse actualizarPerfil(Long idUsuario, ActualizarPerfilRequest request) {
        UsuarioPerfilResponse profile = restClient.put()
                .uri(catalogoUrl + "/api/usuarios/{id}/perfil", idUsuario)
                .header("X-GoTicket-Internal-Token", internalApiToken)
                .body(request)
                .retrieve()
                .body(UsuarioPerfilResponse.class);
        if (profile == null) {
            throw new IllegalStateException("Catalogo-Evento no devolvió el perfil actualizado");
        }
        return profile;
    }
}
