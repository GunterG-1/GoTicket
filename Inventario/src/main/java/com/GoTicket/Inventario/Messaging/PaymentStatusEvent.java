package com.GoTicket.Inventario.Messaging;

import java.math.BigDecimal;

public record PaymentStatusEvent(Long idPago, Long idOrden, Long idEvento, Long idUsuario, Integer cantidad, BigDecimal monto, String estado) {
}