package com.GoTicket.Orden.Client;

public record CancelarReservaRequest(String idReserva, Long idUsuario, Long idOrden) {
}
