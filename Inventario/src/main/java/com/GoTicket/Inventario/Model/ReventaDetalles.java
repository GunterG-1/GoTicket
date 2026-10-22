package com.GoTicket.Inventario.Model;

import java.math.BigDecimal;

public record ReventaDetalles(
        Long idEntrada,
        Long idEvento,
        Long idUsuarioVendedor,
        String sector,
        BigDecimal precio) {
}
