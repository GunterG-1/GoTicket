package com.GoTicket.Pagos.Service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.SnsClient;

import com.GoTicket.Pagos.Model.Pago;
import com.GoTicket.Pagos.Repository.PagoRepository;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {
    @Mock private PagoRepository pagoRepository;
    @Mock private SnsClient snsClient;
    @Mock private ObjectMapper objectMapper;

    private PagoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PagoServiceImpl(pagoRepository, snsClient, objectMapper);
        ReflectionTestUtils.setField(service, "topicArn", "arn:aws:sns:us-east-1:123456789012:test");
        ReflectionTestUtils.setField(service, "approvedKey", "pago.aprobado");
        ReflectionTestUtils.setField(service, "failedKey", "pago.fallido");
        ReflectionTestUtils.setField(service, "paymentExpirationMinutes", 5L);
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
        verifyNoInteractions(pagoRepository, snsClient);
    }

    @Test
    void simulaAprobacionSinDatosDeTarjeta() throws Exception {
        Pago payment = pendingPayment();
        when(pagoRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(payment));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        Pago result = service.simularPago(7L, true, 11L);

        assertEquals("APROBADO", result.getEstado());
        assertEquals("TARJETA", result.getMetodoPago());
        verify(snsClient).publish(any(software.amazon.awssdk.services.sns.model.PublishRequest.class));
    }

    @Test
    void rechazaPagoExpiradoAunqueSeSoliciteAprobacion() throws Exception {
        Pago payment = pendingPayment();
        payment.setFechaCreacion(LocalDateTime.now().minusMinutes(6));
        when(pagoRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(payment));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        Pago result = service.simularPago(7L, true, 11L);

        assertEquals("RECHAZADO", result.getEstado());
        verify(snsClient).publish(any(software.amazon.awssdk.services.sns.model.PublishRequest.class));
    }

    @Test
    void guardaSoloLaMarcaYLosUltimosCuatroDeLaTarjeta() throws Exception {
        Pago payment = pendingPayment();
        when(pagoRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(payment));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        Pago result = service.simularPago(7L, true, 11L, "TARJETA", "visa", "4242");

        assertEquals("TARJETA", result.getMetodoPago());
        assertEquals("VISA", result.getMarcaTarjeta());
        assertEquals("4242", result.getUltimosCuatro());
        verify(snsClient).publish(any(software.amazon.awssdk.services.sns.model.PublishRequest.class));
    }

    @Test
    void noPermiteSimularPagoDeOtroUsuario() {
        when(pagoRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(pendingPayment()));

        assertThrows(IllegalArgumentException.class, () -> service.simularPago(7L, true, 12L));
        verifyNoInteractions(snsClient);
    }

    private Pago pendingPayment() {
        Pago payment = new Pago();
        payment.setIdPago(7L);
        payment.setIdOrden(9L);
        payment.setIdEvento(10L);
        payment.setIdUsuario(11L);
        payment.setCantidad(1);
        payment.setMonto(BigDecimal.TEN);
        payment.setEstado("PENDIENTE");
        payment.setFechaCreacion(LocalDateTime.now());
        return payment;
    }
}
