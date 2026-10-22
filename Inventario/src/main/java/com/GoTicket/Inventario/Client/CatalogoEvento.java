package com.GoTicket.Inventario.Client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CatalogoEvento(
        Long idEvento,
        Integer capacidad,
        BigDecimal precio,
        String nombre,
        String categoria,
        String lugar,
        LocalDateTime fechaInicio,
        Boolean activo) {
}