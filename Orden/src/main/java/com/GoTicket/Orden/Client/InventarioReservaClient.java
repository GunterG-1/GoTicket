package com.GoTicket.Orden.Client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InventarioReservaClient {
    private final RestClient restClient;

    @Value("${inventario.url:http://localhost:8082}")
    private String inventarioUrl;
    @Value("${goticket.internal.api-token:}")
    private String internalApiToken;

    public ReservaDetalles buscarReserva(String idReserva) {
        return restClient.get()
                .uri(inventarioUrl + "/api/inventarios/reservas/{idReserva}", idReserva)
                .header("X-GoTicket-Internal-Token", internalApiToken)
                .retrieve()
                .body(ReservaDetalles.class);
    }

    public ReventaDetalles buscarEntradaEnReventa(Long idEntrada) {
        return restClient.get()
                .uri(inventarioUrl + "/api/inventarios/entradas/{idEntrada}/reventa", idEntrada)
                .header("X-GoTicket-Internal-Token", internalApiToken)
                .retrieve()
                .body(ReventaDetalles.class);
    }

    public void reservarEntradas(ReservarEntradasRequest request) {
        restClient.post()
                .uri(inventarioUrl + "/api/inventarios/reservas")
                .header("X-GoTicket-Internal-Token", internalApiToken)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public void confirmarReserva(ConfirmarReservaRequest request) {
        restClient.post()
                .uri(inventarioUrl + "/api/inventarios/reservas/confirmar")
                .header("X-GoTicket-Internal-Token", internalApiToken)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public void cancelarReserva(String idReserva, Long idUsuario, Long idOrden) {
        restClient.post()
                .uri(inventarioUrl + "/api/inventarios/reservas/cancelar")
                .header("X-GoTicket-Internal-Token", internalApiToken)
                .body(new CancelarReservaRequest(idReserva, idUsuario, idOrden))
                .retrieve()
                .toBodilessEntity();
    }

    public void reservarReventa(Long idEntrada, Long idUsuario, Long idOrden) {
        restClient.post()
                .uri(inventarioUrl + "/api/inventarios/entradas/{idEntrada}/reventa/reservar", idEntrada)
                .header("X-GoTicket-Internal-Token", internalApiToken)
                .body(new ReservarReventaRequest(idUsuario, idOrden))
                .retrieve()
                .toBodilessEntity();
    }

    public void cancelarReservaReventa(Long idEntrada, Long idUsuario, Long idOrden) {
        restClient.post()
                .uri(inventarioUrl + "/api/inventarios/entradas/{idEntrada}/reventa/cancelar", idEntrada)
                .header("X-GoTicket-Internal-Token", internalApiToken)
                .body(new ReservarReventaRequest(idUsuario, idOrden))
                .retrieve()
                .toBodilessEntity();
    }
}
