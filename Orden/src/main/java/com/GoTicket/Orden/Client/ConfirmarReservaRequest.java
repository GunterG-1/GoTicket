package com.GoTicket.Orden.Client;

public record ConfirmarReservaRequest(Long idEvento, Long idUsuario, Long idOrden, String idReserva) {
}
