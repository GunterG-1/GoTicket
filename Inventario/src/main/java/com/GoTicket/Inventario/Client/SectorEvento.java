package com.GoTicket.Inventario.Client;

import java.math.BigDecimal;

public record SectorEvento(
        Long idSector,
        Long idEvento,
        String nombre,
        Integer capacidad,
        BigDecimal precio,
        String modalidad,
        String color,
        Integer posicionX,
        Integer posicionY) {
}
