package com.GoTicket.Orden.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.GoTicket.Orden.Model.Orden;
import com.GoTicket.Orden.Repository.OrdenRepository;
import com.GoTicket.Orden.Repository.OutboxEventRepository;
import com.GoTicket.Orden.Client.InventarioReservaClient;
import com.GoTicket.Orden.Client.ConfirmarReservaRequest;
import com.GoTicket.Orden.Client.ReservaDetalles;
import com.GoTicket.Orden.Client.ReventaDetalles;
import com.GoTicket.Orden.Model.CheckoutRequest;

@ExtendWith(MockitoExtension.class)
class OrdenServiceImplTest {
    @Mock private OrdenRepository ordenRepository;
    @Mock private OutboxEventRepository outboxRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private InventarioReservaClient inventarioReservaClient;

    private OrdenServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrdenServiceImpl(ordenRepository, outboxRepository, objectMapper, inventarioReservaClient);
    }

    @Test
    void rechazaCantidadCero() {
        Orden order = new Orden();
        order.setCantidad(0);
        order.setPrecioUnitario(BigDecimal.TEN);
        assertThrows(RuntimeException.class, () -> service.guardarOrden(order));
        verifyNoInteractions(ordenRepository);
    }

    @Test
    void rechazaPrecioCero() {
        Orden order = new Orden();
        order.setCantidad(1);
        order.setPrecioUnitario(BigDecimal.ZERO);
        assertThrows(RuntimeException.class, () -> service.guardarOrden(order));
        verifyNoInteractions(ordenRepository);
    }

    @Test
    void checkoutReservaCreaOrdenYConfirmaReservaEnInventario() {
        when(inventarioReservaClient.buscarReserva(anyString())).thenReturn(new ReservaDetalles(
                10L, 7L, "General", 2, BigDecimal.TEN, LocalDateTime.now().plusMinutes(4)));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocation -> {
            Orden order = invocation.getArgument(0);
            order.setIdOrden(99L);
            return order;
        });

        Orden order = service.checkout(new CheckoutRequest(10L, 7L, 2, "General"));

        assertEquals(99L, order.getIdOrden());
        assertEquals(BigDecimal.TEN, order.getPrecioUnitario());
        verify(inventarioReservaClient).reservarEntradas(any());
        verify(inventarioReservaClient).confirmarReserva(any(ConfirmarReservaRequest.class));
    }

    @Test
    void checkoutFallaSiNoPuedeConfirmarYCompensaCancelandoReserva() {
        when(inventarioReservaClient.buscarReserva(anyString())).thenReturn(new ReservaDetalles(
                10L, 7L, "General", 1, BigDecimal.TEN, LocalDateTime.now().plusMinutes(4)));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocation -> {
            Orden order = invocation.getArgument(0);
            order.setIdOrden(99L);
            return order;
        });
        org.mockito.Mockito.doThrow(new IllegalStateException("confirmación fallida"))
                .when(inventarioReservaClient).confirmarReserva(any(ConfirmarReservaRequest.class));

        assertThrows(IllegalStateException.class,
                () -> service.checkout(new CheckoutRequest(10L, 7L, 1, "General")));

        verify(inventarioReservaClient).cancelarReserva(anyString(), org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(99L));
    }

    @Test
    void reventaSoloSePuedeComprarMedianteCheckout() {
        Orden resaleOrder = new Orden();
        resaleOrder.setIdEvento(10L);
        resaleOrder.setIdUsuario(7L);
        resaleOrder.setCantidad(1);
        resaleOrder.setIdEntradaReventa(15L);

        assertThrows(IllegalArgumentException.class, () -> service.guardarOrden(resaleOrder));
        verifyNoInteractions(ordenRepository, inventarioReservaClient);
    }

    @Test
    void checkoutDeReventaUsaPrecioPublicadoYReservaLaEntrada() {
        when(inventarioReservaClient.buscarEntradaEnReventa(15L))
                .thenReturn(new ReventaDetalles(15L, 10L, 5L, "General", BigDecimal.TEN));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocation -> {
            Orden order = invocation.getArgument(0);
            order.setIdOrden(100L);
            return order;
        });

        Orden saved = service.checkout(new CheckoutRequest(10L, 7L, 1, "General", 15L));

        assertEquals(BigDecimal.TEN, saved.getPrecioUnitario());
        assertEquals(BigDecimal.TEN, saved.getTotal());
        org.junit.jupiter.api.Assertions.assertNotNull(saved.getExpiraReserva());
        verify(inventarioReservaClient).reservarReventa(15L, 7L, 100L);
    }
}
