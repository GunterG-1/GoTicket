package com.GoTicket.Orden.Client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservaDetalles(
        Long idEvento,
        Long idUsuario,
        String sector,
        Integer cantidad,
        BigDecimal precioUnitario,
        LocalDateTime expiraEn) {
}
