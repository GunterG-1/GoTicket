package com.GoTicket.Pagos.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import com.GoTicket.Pagos.Model.Pago;
import com.GoTicket.Pagos.Repository.PagoRepository;
import com.GoTicket.Pagos.Messaging.PaymentStatusEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Value("${goticket.aws.sns.topic-arn}")
    private String topicArn;
    @Value("${goticket.aws.event.approved}")
    private String approvedKey;
    @Value("${goticket.aws.event.failed}")
    private String failedKey;
    @Value("${goticket.payment.expiration-minutes:5}")
    private long paymentExpirationMinutes;

    @Override
    public List<Pago> listarPagos() {
        return pagoRepository.findAll();
    }

    @Override
    public Optional<Pago> buscarPagoPorId(Long id) {
        return pagoRepository.findById(id);
    }

    @Override
    public List<Pago> buscarPagosPorUsuario(Long idUsuario) {
        return pagoRepository.findByIdUsuario(idUsuario);
    }

    @Override
    public List<Pago> buscarPagosPorOrden(Long idOrden) {
        return pagoRepository.findByIdOrden(idOrden);
    }

    @Override
    @Transactional
    public Pago guardarPago(Pago pago) {
        if (pago.getIdOrden() == null) {
            throw new RuntimeException("La orden es obligatoria");
        }
        if (pagoRepository.existsByIdOrden(pago.getIdOrden())) {
            throw new RuntimeException("La orden ya tiene un intento de pago registrado");
        }
        if (pago.getMonto() == null || pago.getMonto().doubleValue() <= 0) {
            throw new RuntimeException("El monto debe ser mayor a cero");
        }
        if (pago.getMetodoPago() == null || pago.getMetodoPago().isBlank()) {
            throw new RuntimeException("El metodo de pago es obligatorio");
        }

        pago.setEstado("PENDIENTE");
        pago.setMetodoPago("PENDIENTE");
        pago.setFechaCreacion(LocalDateTime.now());
        pago.setFechaActualizacion(LocalDateTime.now());

        return pagoRepository.save(pago);
    }

    @Override
    @Transactional
    public Pago simularPago(Long id, boolean aprobar, Long idUsuario) {
        return simularPago(id, aprobar, idUsuario, "TARJETA", null, null);
    }

    @Override
    @Transactional
    public Pago simularPago(Long id, boolean aprobar, Long idUsuario, String metodoPago,
            String marcaTarjeta, String ultimosCuatro) {
        Pago pago = pagoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado con id: " + id));
        if (idUsuario == null || !idUsuario.equals(pago.getIdUsuario())) {
            throw new IllegalArgumentException("El pago no pertenece al usuario autenticado");
        }
        if (!"PENDIENTE".equalsIgnoreCase(pago.getEstado())) {
            throw new RuntimeException("El pago ya fue procesado");
        }
        String paymentMethod = metodoPago == null || metodoPago.isBlank()
                ? "TARJETA"
                : metodoPago.trim().toUpperCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("TARJETA", "BANCONTACT", "SEPA").contains(paymentMethod)) {
            throw new IllegalArgumentException("El método de pago no está disponible para esta simulación");
        }
        if (ultimosCuatro != null && !ultimosCuatro.matches("\\d{4}")) {
            throw new IllegalArgumentException("Solo se permiten los últimos cuatro dígitos de la tarjeta");
        }
        if (marcaTarjeta != null && !java.util.Set.of("VISA", "MASTERCARD", "AMEX")
                .contains(marcaTarjeta.trim().toUpperCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("La marca de tarjeta no es válida");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime paymentDeadline = pago.getExpiraReserva() != null
                ? pago.getExpiraReserva()
                : pago.getFechaCreacion() == null ? null
                        : pago.getFechaCreacion().plusMinutes(paymentExpirationMinutes);
        boolean expired = paymentDeadline == null || !paymentDeadline.isAfter(now);
        pago.setMetodoPago(paymentMethod);
        pago.setMarcaTarjeta(marcaTarjeta == null ? null : marcaTarjeta.trim().toUpperCase(java.util.Locale.ROOT));
        pago.setUltimosCuatro(ultimosCuatro);
        pago.setIdTransaccion("GT-DEMO-" + java.util.UUID.randomUUID());
        pago.setEstado(expired || !aprobar ? "RECHAZADO" : "APROBADO");
        pago.setFechaActualizacion(now);
        Pago saved = pagoRepository.save(pago);
        publicarResultado(saved);
        return saved;
    }

    private void publicarResultado(Pago pago) {
        String key = "APROBADO".equals(pago.getEstado()) ? approvedKey : failedKey;
        PaymentStatusEvent event = new PaymentStatusEvent(pago.getIdPago(), pago.getIdOrden(), pago.getIdEvento(),
                pago.getIdUsuario(), pago.getCantidad(), pago.getMonto(), pago.getEstado(), pago.getIdEntradaReventa());
        try {
            snsClient.publish(PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(objectMapper.writeValueAsString(event))
                    .messageAttributes(java.util.Map.of(
                            "eventType", software.amazon.awssdk.services.sns.model.MessageAttributeValue.builder()
                                    .dataType("String").stringValue(key).build()))
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo publicar el resultado del pago", exception);
        }
    }

    @Override
    @Transactional
    public Pago actualizarPago(Long id, Pago pago) {
        throw new RuntimeException("Los pagos se procesan mediante el endpoint de simulación");
    }

    @Override
    @Transactional
    public void eliminarPago(Long id) {
        if (!pagoRepository.existsById(id)) {
            throw new RuntimeException("Pago no encontrado con id: " + id);
        }
        pagoRepository.deleteById(id);
    }
}
