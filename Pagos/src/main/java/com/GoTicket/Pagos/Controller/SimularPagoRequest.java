package com.GoTicket.Pagos.Controller;

public record SimularPagoRequest(
        boolean aprobar,
        Long idUsuario,
        String metodoPago,
        String marcaTarjeta,
        String ultimosCuatro) {
}
