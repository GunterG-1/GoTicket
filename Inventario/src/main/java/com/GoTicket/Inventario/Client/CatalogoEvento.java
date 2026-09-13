package com.GoTicket.Inventario.Client;

import java.math.BigDecimal;

public record CatalogoEvento(Long idEvento, Integer capacidad, BigDecimal precio) {
}