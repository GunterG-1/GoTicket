package com.GoTicket.Catalogo.Evento.Service;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.GoTicket.Catalogo.Evento.Model.Evento;

public interface EventoService {

    List<Evento> listarEventos();

    Optional<Evento> buscarEventoPorId(Long id);

    Page<Evento> buscarEventos(String texto, String categoria, String ciudad,
            LocalDateTime desde, LocalDateTime hasta, BigDecimal precioMinimo,
            BigDecimal precioMaximo, Pageable pageable);

    Evento guardarEvento(Evento evento);

    Evento actualizarEvento(Long id, Evento evento);

    void eliminarEvento(Long id);
}
