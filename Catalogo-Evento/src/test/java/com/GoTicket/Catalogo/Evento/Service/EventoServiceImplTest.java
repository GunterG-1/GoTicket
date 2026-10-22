package com.GoTicket.Catalogo.Evento.Service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.GoTicket.Catalogo.Evento.Model.Evento;
import com.GoTicket.Catalogo.Evento.Repository.EventoRepository;
import com.GoTicket.Catalogo.Evento.Repository.RecintoRepository;
import com.GoTicket.Catalogo.Evento.Model.Recinto;

@ExtendWith(MockitoExtension.class)
class EventoServiceImplTest {
    @Mock private EventoRepository eventoRepository;
    @Mock private RecintoRepository recintoRepository;

    private EventoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EventoServiceImpl(eventoRepository, recintoRepository);
    }

    @Test
    void actualizarEventoInexistenteFalla() {
        when(recintoRepository.findById(1L)).thenReturn(Optional.of(
                new Recinto(1L, "Recinto", "Santiago", "Dirección", 200)));
        when(eventoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.actualizarEvento(99L, eventoValido()));
    }

    @Test
    void eliminarEventoInexistenteFalla() {
        when(eventoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.eliminarEvento(99L));
    }

    @Test
    void actualizarEventoRechazaPrecioNegativo() {
        Evento event = eventoValido();
        event.setPrecio(java.math.BigDecimal.valueOf(-1));

        assertThrows(IllegalArgumentException.class, () -> service.actualizarEvento(1L, event));
    }

    @Test
    void eliminarEventoLoDesactivaEnLugarDeBorrarlo() {
        Evento event = eventoValido();
        event.setIdEvento(1L);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.eliminarEvento(1L);

        assertFalse(event.getActivo());
        verify(eventoRepository).save(event);
    }

    @Test
    void actualizarEventoAceptaDatosValidos() {
        Evento existing = eventoValido();
        existing.setIdEvento(1L);
        Evento update = eventoValido();
        update.setNombre("Evento actualizado");
        when(recintoRepository.findById(1L)).thenReturn(Optional.of(new Recinto(1L, "Recinto", "Santiago", "Dirección", 200)));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("Evento actualizado", service.actualizarEvento(1L, update).getNombre());
    }

    @Test
    void guardarEventoDerivaLugarDesdeRecintoSeleccionado() {
        Evento event = eventoValido();
        event.setLugar(null);
        when(recintoRepository.findById(1L))
                .thenReturn(Optional.of(new Recinto(1L, "Recinto", "Santiago", "Dirección", 200)));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Evento saved = service.guardarEvento(event);

        assertEquals("Recinto, Santiago", saved.getLugar());
    }

    @Test
    void buscarEventosLimpiaTextoYDevuelvePagina() {
        Evento event = eventoValido();
        when(eventoRepository.buscarEventos("concierto", null, "santiago", null, null,
                null, null, PageRequest.of(0, 12)))
                .thenReturn(new PageImpl<>(List.of(event)));

        var result = service.buscarEventos(" concierto ", null, " santiago ",
                null, null, null, null, PageRequest.of(0, 12));

        assertEquals(1, result.getTotalElements());
        assertEquals("Evento", result.getContent().get(0).getNombre());
    }

    @Test
    void buscarEventosRechazaRangosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> service.buscarEventos(null, null, null,
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(1),
                null, null, PageRequest.of(0, 12)));
    }

    private Evento eventoValido() {
        Evento event = new Evento();
        event.setNombre("Evento");
        event.setCategoria("Música");
        event.setLugar("Recinto");
        event.setFechaInicio(java.time.LocalDateTime.now().plusDays(1));
        event.setCapacidad(100);
        event.setIdRecinto(1L);
        event.setPrecio(java.math.BigDecimal.TEN);
        event.setActivo(true);
        return event;
    }
}
