package com.GoTicket.Catalogo.Evento.Model;

public record IdentidadGoogleRequest(
        Long idUsuario,
        String googleSubject,
        String email,
        String nombre) {
}
