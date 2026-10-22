package com.GoTicket.Pagos.Messaging;

import com.GoTicket.Pagos.Model.Pago;
import com.GoTicket.Pagos.Repository.PagoRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PaymentExpirationListener {
    private final PagoRepository pagoRepository;

    @Transactional
    public void handle(PaymentStatusEvent event) {
        if (event.idOrden() == null) return;
        pagoRepository.findByIdOrden(event.idOrden()).stream().findFirst()
                .flatMap(existing -> pagoRepository.findByIdForUpdate(existing.getIdPago())).ifPresent(pago -> {
            if ("PENDIENTE".equalsIgnoreCase(pago.getEstado())) {
                pago.setEstado("RECHAZADO");
                pago.setMetodoPago("EXPIRADO");
                pago.setFechaActualizacion(LocalDateTime.now());
                pagoRepository.save(pago);
            }
        });
    }
}
