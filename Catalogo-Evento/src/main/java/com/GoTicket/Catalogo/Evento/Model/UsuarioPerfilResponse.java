package com.GoTicket.Catalogo.Evento.Model;

public record UsuarioPerfilResponse(
        Long idUsuario,
        String nombre,
        String email,
        String rut,
        String telefono) {
}
