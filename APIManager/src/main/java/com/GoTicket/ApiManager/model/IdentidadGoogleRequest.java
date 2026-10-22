package com.GoTicket.ApiManager.model;

public record IdentidadGoogleRequest(
        Long idUsuario,
        String googleSubject,
        String email,
        String nombre) {
}
