package com.GoTicket.Inventario.Client;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CatalogoEventoClient {
    private final RestClient restClient;

    @Value("${catalogo.url:http://localhost:8081}")
    private String catalogoUrl;

    public Optional<CatalogoEvento> buscarEvento(Long idEvento) {
        try {
            CatalogoEvento evento = restClient.get()
                    .uri(catalogoUrl + "/api/eventos/{id}", idEvento)
                    .retrieve()
                    .body(CatalogoEvento.class);
            return Optional.ofNullable(evento);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }
}