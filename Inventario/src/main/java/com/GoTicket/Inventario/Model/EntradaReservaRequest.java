package com.GoTicket.Inventario.Model;

public record EntradaReservaRequest(Long idEvento, Long idUsuario, String sector, Integer cantidad) {
}