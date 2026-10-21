package com.GoTicket.Inventario.Messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EstadoEntrada;
import com.GoTicket.Inventario.Repository.BloqueoInventarioRepository;
import com.GoTicket.Inventario.Repository.EntradaRepository;
import com.GoTicket.Inventario.Repository.InventarioRepository;
import com.GoTicket.Inventario.Repository.TransferenciaEntradaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentStatusListenerTest {
    @Mock private InventarioRepository inventarioRepository;
    @Mock private BloqueoInventarioRepository bloqueoRepository;
    @Mock private EntradaRepository entradaRepository;
    @Mock private TransferenciaEntradaRepository transferenciaRepository;
    @Mock private StringRedisTemplate redisTemplate;

    @Test
    void pagoAprobadoTransfiereLaEntradaDeReventaAlComprador() {
        Entrada entry = new Entrada(15L, 10L, "General", "1", BigDecimal.TEN,
                EstadoEntrada.EN_REVENTA, 5L, 99L, null, null);
        when(entradaRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(entry));
        when(entradaRepository.save(any(Entrada.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentStatusListener listener = new PaymentStatusListener(inventarioRepository, bloqueoRepository,
                entradaRepository, transferenciaRepository, redisTemplate);
        listener.approved(new PaymentStatusEvent(1L, 99L, 10L, 7L, 1,
                BigDecimal.TEN, "APROBADO", 15L));

        assertEquals(7L, entry.getIdUsuario());
        assertEquals(EstadoEntrada.VENDIDO, entry.getEstado());
        assertEquals(99L, entry.getIdOrden());
        verify(transferenciaRepository).save(any());
    }

    @Test
    void pagoFallidoDevuelveLaEntradaAlMarketplace() {
        Entrada entry = new Entrada(15L, 10L, "General", "1", BigDecimal.TEN,
                EstadoEntrada.EN_REVENTA, 5L, 99L, null, null);
        when(entradaRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(entry));
        when(entradaRepository.save(any(Entrada.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentStatusListener listener = new PaymentStatusListener(inventarioRepository, bloqueoRepository,
                entradaRepository, transferenciaRepository, redisTemplate);
        listener.failed(new PaymentStatusEvent(1L, 99L, 10L, 7L, 1,
                BigDecimal.TEN, "RECHAZADO", 15L));

        assertEquals(5L, entry.getIdUsuario());
        assertEquals(EstadoEntrada.EN_REVENTA, entry.getEstado());
        assertNull(entry.getIdOrden());
    }
}
