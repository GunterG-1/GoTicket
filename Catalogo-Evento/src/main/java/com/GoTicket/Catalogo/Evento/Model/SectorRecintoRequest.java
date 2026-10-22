package com.GoTicket.Catalogo.Evento.Model;

public record SectorRecintoRequest(
        String nombre,
        Integer capacidad,
        String modalidad,
        String color,
        Integer posicionX,
        Integer posicionY) {
}
