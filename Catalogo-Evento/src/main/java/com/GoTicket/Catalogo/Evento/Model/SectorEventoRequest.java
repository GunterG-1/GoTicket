package com.GoTicket.Catalogo.Evento.Model;

import java.math.BigDecimal;

public record SectorEventoRequest(
        String nombre,
        Integer capacidad,
        BigDecimal precio,
        String modalidad,
        String color,
        Integer posicionX,
        Integer posicionY) {
}
