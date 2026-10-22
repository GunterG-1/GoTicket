package com.GoTicket.Orden.Client;

public record ReservarEntradasRequest(
        Long idEvento,
        Long idUsuario,
        String sector,
        Integer cantidad,
        String idReserva) {
}
