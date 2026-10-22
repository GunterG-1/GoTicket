package com.GoTicket.Inventario.Model;

import java.math.BigDecimal;

public record SectorDisponibilidad(String sector, int cantidadTotal, int cantidadDisponible,
        BigDecimal precio, String modalidad, String color, Integer posicionX, Integer posicionY) {
}
