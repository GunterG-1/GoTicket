package com.GoTicket.Pagos.Service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.GoTicket.Pagos.Model.Pago;
import com.GoTicket.Pagos.Repository.PagoRepository;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {
    @Mock private PagoRepository pagoRepository;
    @Mock private RabbitTemplate rabbitTemplate;

    private PagoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PagoServiceImpl(pagoRepository, rabbitTemplate);
    }

    @Test
    void rechazaPagoSinOrden() {
        Pago payment = new Pago();
        payment.setMonto(BigDecimal.TEN);
        payment.setMetodoPago("DEMO");

        assertThrows(RuntimeException.class, () -> service.guardarPago(payment));
        verifyNoInteractions(pagoRepository);
    }

    @Test
    void rechazaPagoDuplicadoPorOrden() {
        Pago payment = new Pago();
        payment.setIdOrden(20L);
        payment.setMonto(BigDecimal.TEN);
        payment.setMetodoPago("DEMO");
        when(pagoRepository.existsByIdOrden(20L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> service.guardarPago(payment));
    }

    @Test
    void noPermiteModificarPagoFinalizado() {
        assertThrows(RuntimeException.class, () -> service.actualizarPago(3L, new Pago()));
        verifyNoInteractions(pagoRepository, rabbitTemplate);
    }
}
