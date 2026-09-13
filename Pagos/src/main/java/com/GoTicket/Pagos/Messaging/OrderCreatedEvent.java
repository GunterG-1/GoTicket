package com.GoTicket.Pagos.Messaging;

import java.math.BigDecimal;

public record OrderCreatedEvent(Long idOrden, Long idEvento, Long idUsuario, Integer cantidad, BigDecimal total, String sector) {
}