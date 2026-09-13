package com.GoTicket.Inventario.Model;

import java.math.BigDecimal;

public record PublicarReventaRequest(Long idUsuario, BigDecimal precio) {
}