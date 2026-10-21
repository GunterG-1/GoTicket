package com.GoTicket.Orden.Service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.GoTicket.Orden.Model.Orden;
import com.GoTicket.Orden.Repository.OrdenRepository;
import com.GoTicket.Orden.Repository.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
class OrdenServiceImplTest {
    @Mock private OrdenRepository ordenRepository;
    @Mock private OutboxEventRepository outboxRepository;
    @Mock private ObjectMapper objectMapper;

    private OrdenServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrdenServiceImpl(ordenRepository, outboxRepository, objectMapper);
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
}
