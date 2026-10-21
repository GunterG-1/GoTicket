package com.GoTicket.Inventario.Messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.GoTicket.Inventario.Model.BloqueoInventario;
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
        when(inventarioRepository.findByIdEventoForUpdate(10L)).thenReturn(Optional.of(inventory));
        when(bloqueoRepository.findByIdOrden(99L))
                .thenReturn(Optional.of(new BloqueoInventario(1L, 99L, 10L, 1, null, false)));

        OrderCreatedListener listener = new OrderCreatedListener(
                inventarioRepository, bloqueoRepository, entradaRepository);
        listener.handle(new OrderCreatedEvent(99L, 10L, 7L, 1, BigDecimal.TEN, "General", null));

        assertEquals(9, inventory.getCantidadDisponible());
        verify(inventarioRepository, never()).save(any(Inventario.class));
        verify(bloqueoRepository, never()).save(any(BloqueoInventario.class));
    }
}
