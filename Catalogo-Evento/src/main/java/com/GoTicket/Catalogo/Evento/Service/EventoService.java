package com.GoTicket.Catalogo.Evento.Service;

import java.util.List;
import java.util.Optional;

import com.GoTicket.Catalogo.Evento.Model.Evento;

public interface EventoService {

    List<Evento> listarEventos();

    Optional<Evento> buscarEventoPorId(Long id);

    Evento guardarEvento(Evento evento);

    Evento actualizarEvento(Long id, Evento evento);

    void eliminarEvento(Long id);
}
