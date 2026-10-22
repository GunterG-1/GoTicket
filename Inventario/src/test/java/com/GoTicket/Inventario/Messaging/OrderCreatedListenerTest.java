package com.GoTicket.Inventario.Messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.GoTicket.Inventario.Model.BloqueoInventario;
import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EstadoEntrada;
import com.GoTicket.Inventario.Model.Inventario;
import com.GoTicket.Inventario.Repository.BloqueoInventarioRepository;
import com.GoTicket.Inventario.Repository.EntradaRepository;
import com.GoTicket.Inventario.Repository.InventarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderCreatedListenerTest {
    @Mock private InventarioRepository inventarioRepository;
    @Mock private BloqueoInventarioRepository bloqueoRepository;
    @Mock private EntradaRepository entradaRepository;

    @Test
    void eventoRepetidoNoDescuentaDisponibilidadOtraVez() {
        Inventario inventory = new Inventario();
        inventory.setCantidadDisponible(9);
        inventory.setCantidadTotal(10);
        when(bloqueoRepository.findByIdOrden(99L))
                .thenReturn(Optional.of(new BloqueoInventario(1L, 99L, 10L, 1, null, false)));

        OrderCreatedListener listener = new OrderCreatedListener(
                inventarioRepository, bloqueoRepository, entradaRepository);
        listener.handle(new OrderCreatedEvent(99L, 10L, 7L, 1, BigDecimal.TEN,
                "General", null, null, null));

        assertEquals(9, inventory.getCantidadDisponible());
        verify(inventarioRepository, never()).save(any(Inventario.class));
        verify(bloqueoRepository, never()).save(any(BloqueoInventario.class));
    }

    @Test
    void ordenValidaAsociaEntradasYUsaVencimientoDeReserva() {
        LocalDateTime expiration = LocalDateTime.now().plusMinutes(4);
        Entrada entry = new Entrada(15L, 10L, "General", "1", BigDecimal.TEN, BigDecimal.TEN,
                EstadoEntrada.BLOQUEADO, 7L, null, "reserva-1", null, LocalDateTime.now(), expiration, null, null);
        Inventario inventory = new Inventario();
        inventory.setCantidadDisponible(9);
        inventory.setCantidadTotal(10);
        when(entradaRepository.findByIdEventoAndIdUsuarioAndIdReservaAndEstado(
                10L, 7L, "reserva-1", EstadoEntrada.BLOQUEADO)).thenReturn(List.of(entry));
        when(bloqueoRepository.findByIdOrden(99L)).thenReturn(Optional.empty());
        when(inventarioRepository.findByIdEventoForUpdate(10L)).thenReturn(Optional.of(inventory));

        OrderCreatedListener listener = new OrderCreatedListener(
                inventarioRepository, bloqueoRepository, entradaRepository);
        listener.handle(new OrderCreatedEvent(99L, 10L, 7L, 1, BigDecimal.TEN,
                "General", null, "reserva-1", expiration));

        assertEquals(8, inventory.getCantidadDisponible());
        assertEquals(99L, entry.getIdOrden());
        verify(entradaRepository).saveAll(List.of(entry));
        verify(bloqueoRepository).save(org.mockito.ArgumentMatchers.argThat(
                lock -> expiration.equals(lock.getExpiraEn()) && lock.getCantidad() == 1));
    }

    @Test
    void rechazaOrdenConMontoDistintoAlDeLaReserva() {
        LocalDateTime expiration = LocalDateTime.now().plusMinutes(4);
        Entrada entry = new Entrada(15L, 10L, "General", "1", BigDecimal.TEN, BigDecimal.TEN,
                EstadoEntrada.BLOQUEADO, 7L, null, "reserva-1", null, LocalDateTime.now(), expiration, null, null);
        when(entradaRepository.findByIdEventoAndIdUsuarioAndIdReservaAndEstado(
                10L, 7L, "reserva-1", EstadoEntrada.BLOQUEADO)).thenReturn(List.of(entry));
        when(bloqueoRepository.findByIdOrden(99L)).thenReturn(Optional.empty());

        OrderCreatedListener listener = new OrderCreatedListener(
                inventarioRepository, bloqueoRepository, entradaRepository);
        assertThrows(IllegalStateException.class, () -> listener.handle(new OrderCreatedEvent(
                99L, 10L, 7L, 1, BigDecimal.valueOf(11), "General", null, "reserva-1", expiration)));
    }

    @Test
    void eventoDeReventaDebeCoincidirConElBloqueoDelComprador() {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(4);
        Entrada resale = new Entrada(15L, 10L, "General", "1", BigDecimal.TEN, BigDecimal.TEN,
                EstadoEntrada.EN_REVENTA, 5L, 99L, null, 7L, LocalDateTime.now(), expiresAt, null, null);
        when(entradaRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(resale));
        OrderCreatedListener listener = new OrderCreatedListener(
                inventarioRepository, bloqueoRepository, entradaRepository);

        listener.handle(new OrderCreatedEvent(99L, 10L, 7L, 1, BigDecimal.TEN,
                "General", 15L, null, expiresAt.minusSeconds(1)));

        verify(entradaRepository, never()).save(any(Entrada.class));
    }

    @Test
    void rechazaEventoDeReventaParaOrdenDistintaAlBloqueo() {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(4);
        Entrada resale = new Entrada(15L, 10L, "General", "1", BigDecimal.TEN, BigDecimal.TEN,
                EstadoEntrada.EN_REVENTA, 5L, 99L, null, 7L, LocalDateTime.now(), expiresAt, null, null);
        when(entradaRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(resale));
        OrderCreatedListener listener = new OrderCreatedListener(
                inventarioRepository, bloqueoRepository, entradaRepository);

        assertThrows(IllegalStateException.class, () -> listener.handle(new OrderCreatedEvent(
                100L, 10L, 7L, 1, BigDecimal.TEN, "General", 15L, null, expiresAt.minusSeconds(1))));
    }
}
