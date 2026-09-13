package com.GoTicket.Orden.Messaging;

import java.math.BigDecimal;

public record PaymentStatusEvent(Long idPago, Long idOrden, Long idEvento, Long idUsuario, Integer cantidad, BigDecimal monto, String estado) {
}