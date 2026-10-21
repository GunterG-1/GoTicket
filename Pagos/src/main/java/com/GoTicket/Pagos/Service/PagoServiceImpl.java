package com.GoTicket.Pagos.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Pago procesarPago(Long id, String numeroTarjeta, String vencimiento, String cvv, String titular) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado con id: " + id));
        if (!"PENDIENTE".equalsIgnoreCase(pago.getEstado())) {
            throw new RuntimeException("El pago ya fue procesado");
        }
        if (pago.getFechaCreacion() == null || pago.getFechaCreacion().plusMinutes(paymentExpirationMinutes).isBefore(LocalDateTime.now())) {
            pago.setEstado("RECHAZADO");
            pago.setMetodoPago("EXPIRADO");
            pago.setFechaActualizacion(LocalDateTime.now());
            Pago expirado = pagoRepository.save(pago);
            publicarResultado(expirado);
            throw new RuntimeException("El tiempo para pagar expiro");
        }
        validarDatosTarjeta(numeroTarjeta, vencimiento, cvv, titular);

        String tarjeta = numeroTarjeta.replaceAll("\\s+", "");
        pago.setMetodoPago("TARJETA");
        pago.setMarcaTarjeta(detectarMarca(tarjeta));
        pago.setUltimosCuatro(tarjeta.substring(tarjeta.length() - 4));
        pago.setIdTransaccion("GT-" + UUID.randomUUID());
        pago.setEstado(tarjeta.endsWith("0002") ? "RECHAZADO" : "APROBADO");
        pago.setFechaActualizacion(LocalDateTime.now());
        Pago guardado = pagoRepository.save(pago);
        publicarResultado(guardado);
        return guardado;
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

    private void validarDatosTarjeta(String numero, String vencimiento, String cvv, String titular) {
        if (titular == null || titular.isBlank() || vencimiento == null || !vencimiento.matches("(0[1-9]|1[0-2])/((\\d{2})|(20\\d{2}))")
                || cvv == null || !cvv.matches("\\d{3,4}")) {
            throw new RuntimeException("Revisa los datos de la tarjeta");
        }
        String tarjeta = numero == null ? "" : numero.replaceAll("\\s+", "");
        if (!tarjeta.matches("\\d{13,19}") || !luhnValida(tarjeta)) {
            throw new RuntimeException("El numero de tarjeta no es valido");
        }
    }

    private boolean luhnValida(String numero) {
        int suma = 0;
        boolean duplicar = false;
        for (int indice = numero.length() - 1; indice >= 0; indice--) {
            int digito = numero.charAt(indice) - '0';
            if (duplicar && (digito *= 2) > 9) digito -= 9;
            suma += digito;
            duplicar = !duplicar;
        }
        return suma % 10 == 0;
    }

    private String detectarMarca(String numero) {
        if (numero.startsWith("4")) return "VISA";
        if (numero.matches("5[1-5].*")) return "MASTERCARD";
        if (numero.matches("3[47].*")) return "AMEX";
        return "TARJETA";
    }

    @Override
    @Transactional
    public Pago actualizarPago(Long id, Pago pago) {
        throw new RuntimeException("Los pagos se procesan mediante el endpoint /procesar");
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
