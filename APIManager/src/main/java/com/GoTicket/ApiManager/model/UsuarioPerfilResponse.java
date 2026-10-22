package com.GoTicket.ApiManager.model;

public record UsuarioPerfilResponse(
        Long idUsuario,
        String nombre,
        String email,
        String rut,
        String telefono) {
}
