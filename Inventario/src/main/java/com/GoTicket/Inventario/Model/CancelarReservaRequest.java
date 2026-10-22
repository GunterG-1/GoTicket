package com.GoTicket.Inventario.Model;

public record CancelarReservaRequest(String idReserva, Long idUsuario, Long idOrden) {
}
