package com.GoTicket.Pagos.Controller;

public record ProcesarPagoRequest(String numeroTarjeta, String vencimiento, String cvv, String titular) {
}