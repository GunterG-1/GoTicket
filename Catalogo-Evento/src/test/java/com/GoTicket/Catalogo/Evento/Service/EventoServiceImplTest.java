package com.GoTicket.Catalogo.Evento.Service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.GoTicket.Catalogo.Evento.Model.Evento;
import com.GoTicket.Catalogo.Evento.Repository.EventoRepository;

@ExtendWith(MockitoExtension.class)
class EventoServiceImplTest {
    @Mock private EventoRepository eventoRepository;

    private EventoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EventoServiceImpl(eventoRepository);
    }

    @Test
    void actualizarEventoInexistenteFalla() {
        when(eventoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.actualizarEvento(99L, new Evento()));
    }

    @Test
    void eliminarEventoInexistenteFalla() {
        when(eventoRepository.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.eliminarEvento(99L));
    }
}
