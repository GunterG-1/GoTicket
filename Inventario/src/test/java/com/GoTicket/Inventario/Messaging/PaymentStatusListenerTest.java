package com.GoTicket.Inventario.Messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EstadoEntrada;
import com.GoTicket.Inventario.Model.TransferenciaEntrada;
import com.GoTicket.Inventario.Repository.BloqueoInventarioRepository;
import com.GoTicket.Inventario.Repository.EntradaRepository;
import com.GoTicket.Inventario.Repository.InventarioRepository;
import com.GoTicket.Inventario.Repository.TransferenciaEntradaRepository;
import com.GoTicket.Inventario.Service.TicketNotificationService;
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
    @Mock private TicketNotificationService ticketNotificationService;

    @Test
    void pagoAprobadoTransfiereLaEntradaDeReventaAlComprador() {
        Entrada entry = entradaDeReventa();
        when(entradaRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(entry));
        when(entradaRepository.save(any(Entrada.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentStatusListener listener = new PaymentStatusListener(inventarioRepository, bloqueoRepository,
                entradaRepository, transferenciaRepository, redisTemplate, ticketNotificationService);
        listener.approved(new PaymentStatusEvent(1L, 99L, 10L, 7L, 1,
                BigDecimal.TEN, "APROBADO", 15L));

        assertEquals(7L, entry.getIdUsuario());
        assertEquals(EstadoEntrada.VENDIDO, entry.getEstado());
        assertEquals(99L, entry.getIdOrden());
        assertNotNull(entry.getCodigoAcceso());
        var transferCaptor = forClass(TransferenciaEntrada.class);
        verify(transferenciaRepository).save(transferCaptor.capture());
        assertEquals(new BigDecimal("0.50"), transferCaptor.getValue().getComisionSimulada());
        assertEquals(new BigDecimal("9.50"), transferCaptor.getValue().getNetoVendedorSimulado());
    }

    @Test
    void pagoFallidoDevuelveLaEntradaAlMarketplace() {
        Entrada entry = entradaDeReventa();
        when(entradaRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(entry));
        when(entradaRepository.save(any(Entrada.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentStatusListener listener = new PaymentStatusListener(inventarioRepository, bloqueoRepository,
                entradaRepository, transferenciaRepository, redisTemplate, ticketNotificationService);
        listener.failed(new PaymentStatusEvent(1L, 99L, 10L, 7L, 1,
                BigDecimal.TEN, "RECHAZADO", 15L));

        assertEquals(5L, entry.getIdUsuario());
        assertEquals(EstadoEntrada.EN_REVENTA, entry.getEstado());
        assertNull(entry.getIdOrden());
    }

    private Entrada entradaDeReventa() {
        LocalDateTime now = LocalDateTime.now();
        Entrada entry = new Entrada();
        entry.setIdEntrada(15L);
        entry.setIdEvento(10L);
        entry.setSector("General");
        entry.setNumeroAsiento("1");
        entry.setPrecio(BigDecimal.TEN);
        entry.setPrecioOriginal(BigDecimal.TEN);
        entry.setEstado(EstadoEntrada.EN_REVENTA);
        entry.setIdUsuario(5L);
        entry.setIdOrden(99L);
        entry.setIdUsuarioReserva(7L);
        entry.setFechaBloqueo(now);
        entry.setExpiraEn(now.plusMinutes(5));
        return entry;
    }
}
