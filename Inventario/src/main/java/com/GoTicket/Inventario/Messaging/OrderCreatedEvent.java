package com.GoTicket.Inventario.Messaging;

public record OrderCreatedEvent(Long idOrden, Long idEvento, Integer cantidad) {
}