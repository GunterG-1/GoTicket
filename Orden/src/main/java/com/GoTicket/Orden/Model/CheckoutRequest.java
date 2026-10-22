package com.GoTicket.Orden.Model;

public record CheckoutRequest(
        Long idEvento,
        Long idUsuario,
        Integer cantidad,
        String sector,
        Long idEntradaReventa) {
    public CheckoutRequest(Long idEvento, Long idUsuario, Integer cantidad, String sector) {
        this(idEvento, idUsuario, cantidad, sector, null);
    }
}
