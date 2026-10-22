package com.GoTicket.Catalogo.Evento.Service;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.GoTicket.Catalogo.Evento.Model.Evento;
import com.GoTicket.Catalogo.Evento.Model.Recinto;
import com.GoTicket.Catalogo.Evento.Repository.EventoRepository;
import com.GoTicket.Catalogo.Evento.Repository.RecintoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventoServiceImpl implements EventoService {

    private final EventoRepository eventoRepository;
    private final RecintoRepository recintoRepository;

    @Override
    public List<Evento> listarEventos() {
        return eventoRepository.findAll();
    }

    @Override
    public Optional<Evento> buscarEventoPorId(Long id) {
        return eventoRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Evento> buscarEventos(String texto, String categoria, String ciudad,
            LocalDateTime desde, LocalDateTime hasta, BigDecimal precioMinimo,
            BigDecimal precioMaximo, Pageable pageable) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial");
        }
        if (precioMinimo != null && precioMinimo.signum() < 0
                || precioMaximo != null && precioMaximo.signum() < 0
                || precioMinimo != null && precioMaximo != null && precioMaximo.compareTo(precioMinimo) < 0) {
            throw new IllegalArgumentException("El rango de precios no es válido");
        }
        Pageable boundedPageable = PageRequest.of(pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 50), pageable.getSort());
        return eventoRepository.buscarEventos(limpiar(texto), limpiar(categoria), limpiar(ciudad),
                desde, hasta, precioMinimo, precioMaximo, boundedPageable);
    }

    private String limpiar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Override
    @Transactional
    public Evento guardarEvento(Evento evento) {
        validarEvento(evento);
        aplicarRecinto(evento);
        return eventoRepository.save(evento);
    }

    private void validarEvento(Evento evento) {
        if (evento == null) {
            throw new IllegalArgumentException("Los datos del evento son obligatorios");
        }
        if (evento.getNombre() == null || evento.getNombre().isBlank()
                || evento.getCategoria() == null || evento.getCategoria().isBlank()
                || evento.getFechaInicio() == null) {
            throw new IllegalArgumentException("Nombre, categoría y fecha de inicio son obligatorios");
        }
        if (evento.getFechaFin() != null && evento.getFechaFin().isBefore(evento.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha de término no puede ser anterior al inicio");
        }
        if (evento.getCapacidad() == null || evento.getCapacidad() <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor que cero");
        }
        if (evento.getPrecio() == null || evento.getPrecio().signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
    }

    private void aplicarRecinto(Evento evento) {
        if (evento.getIdRecinto() == null) {
            throw new IllegalArgumentException("Debes seleccionar un recinto configurado");
        }
        Recinto venue = recintoRepository.findById(evento.getIdRecinto())
                .orElseThrow(() -> new IllegalArgumentException("El recinto seleccionado no existe"));
        if (evento.getCapacidad() > venue.getCapacidad()) {
            throw new IllegalArgumentException("La capacidad del evento supera la capacidad del recinto");
        }
        evento.setLugar(venue.getNombre() + ", " + venue.getCiudad());
    }

    @Override
    @Transactional
    public Evento actualizarEvento(Long id, Evento evento) {
        validarEvento(evento);
        aplicarRecinto(evento);
        Evento eventoExistente = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + id));

        eventoExistente.setNombre(evento.getNombre());
        eventoExistente.setDescripcion(evento.getDescripcion());
        eventoExistente.setCategoria(evento.getCategoria());
        eventoExistente.setImagenUrl(evento.getImagenUrl());
        eventoExistente.setUrlEvento(evento.getUrlEvento());
        eventoExistente.setFechaInicio(evento.getFechaInicio());
        eventoExistente.setFechaFin(evento.getFechaFin());
        eventoExistente.setLugar(evento.getLugar());
        eventoExistente.setIdRecinto(evento.getIdRecinto());
        eventoExistente.setCapacidad(evento.getCapacidad());
        eventoExistente.setPrecio(evento.getPrecio());
        if (evento.getActivo() != null) {
            eventoExistente.setActivo(evento.getActivo());
        }

        return eventoRepository.save(eventoExistente);
    }

    @Override
    @Transactional
    public void eliminarEvento(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + id));
        evento.setActivo(false);
        eventoRepository.save(evento);
    }
}
