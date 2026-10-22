package com.GoTicket.Inventario.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EntradaUsuarioResponse(
        Long idEntrada,
        Long idEvento,
        String sector,
        String numeroAsiento,
        BigDecimal precio,
        EstadoEntrada estado,
        LocalDateTime fechaUso,
        String codigoAcceso) {
}
