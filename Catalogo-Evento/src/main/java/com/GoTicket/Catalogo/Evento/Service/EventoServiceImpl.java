package com.GoTicket.Catalogo.Evento.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.GoTicket.Catalogo.Evento.Model.Evento;
import com.GoTicket.Catalogo.Evento.Repository.EventoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventoServiceImpl implements EventoService {

    private final EventoRepository eventoRepository;

    @Override
    public List<Evento> listarEventos() {
        return eventoRepository.findAll();
    }

    @Override
    public Optional<Evento> buscarEventoPorId(Long id) {
        return eventoRepository.findById(id);
    }

    @Override
    @Transactional
    public Evento guardarEvento(Evento evento) {
        validarEvento(evento);
        return eventoRepository.save(evento);
    }

    private void validarEvento(Evento evento) {
        if (evento.getNombre() == null || evento.getNombre().isBlank()
                || evento.getCategoria() == null || evento.getCategoria().isBlank()
                || evento.getLugar() == null || evento.getLugar().isBlank()
                || evento.getFechaInicio() == null) {
            throw new IllegalArgumentException("Nombre, categoría, lugar y fecha de inicio son obligatorios");
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

    @Override
    @Transactional
    public Evento actualizarEvento(Long id, Evento evento) {
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
        eventoExistente.setCapacidad(evento.getCapacidad());
        eventoExistente.setPrecio(evento.getPrecio());
        eventoExistente.setActivo(evento.getActivo());

        return eventoRepository.save(eventoExistente);
    }

    @Override
    @Transactional
    public void eliminarEvento(Long id) {
        if (!eventoRepository.existsById(id)) {
            throw new RuntimeException("Evento no encontrado con id: " + id);
        }
        eventoRepository.deleteById(id);
    }
}
