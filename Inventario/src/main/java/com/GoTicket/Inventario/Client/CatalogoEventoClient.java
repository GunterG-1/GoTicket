package com.GoTicket.Inventario.Client;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CatalogoEventoClient {
    private final RestClient restClient;

    @Value("${catalogo.url:http://localhost:8081}")
    private String catalogoUrl;
    @Value("${goticket.internal.api-token:}")
    private String internalApiToken;

    public Optional<CatalogoEvento> buscarEvento(Long idEvento) {
        try {
            CatalogoEvento evento = restClient.get()
                    .uri(catalogoUrl + "/api/eventos/{id}", idEvento)
                    .header("X-GoTicket-Internal-Token", internalApiToken)
                    .retrieve()
                    .body(CatalogoEvento.class);
            return Optional.ofNullable(evento);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public List<CatalogoEvento> listarEventos() {
        try {
            CatalogoEvento[] eventos = restClient.get()
                    .uri(catalogoUrl + "/api/eventos")
                    .header("X-GoTicket-Internal-Token", internalApiToken)
                    .retrieve()
                    .body(CatalogoEvento[].class);
            return eventos == null ? List.of() : List.of(eventos);
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    public List<SectorEvento> listarSectores(Long idEvento) {
        List<SectorEvento> sectors = restClient.get()
                .uri(catalogoUrl + "/api/eventos/{id}/sectores", idEvento)
                .header("X-GoTicket-Internal-Token", internalApiToken)
                .retrieve()
                .body(ParameterizedTypeReference.forType(
                        ResolvableType.forClassWithGenerics(List.class, SectorEvento.class).getType()));
        if (sectors == null) {
            throw new IllegalStateException("Catalogo-Evento devolvió una respuesta vacía de sectores");
        }
        return sectors;
    }

    public CatalogoUsuario buscarUsuario(Long idUsuario) {
        CatalogoUsuario user = restClient.get()
                .uri(catalogoUrl + "/api/usuarios/{id}", idUsuario)
                .header("X-GoTicket-Internal-Token", internalApiToken)
                .retrieve()
                .body(CatalogoUsuario.class);
        if (user == null) {
            throw new IllegalStateException("Catalogo-Evento devolvió una respuesta vacía de usuario");
        }
        return user;
    }
}