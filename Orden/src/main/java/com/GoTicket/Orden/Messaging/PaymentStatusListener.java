package com.GoTicket.Orden.Messaging;

import com.GoTicket.Orden.Model.Orden;
import com.GoTicket.Orden.Repository.OrdenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PaymentStatusListener {
    private final OrdenRepository ordenRepository;

    @Transactional
    public void approved(PaymentStatusEvent event) { update(event.idOrden(), "COMPLETADO"); }

    @Transactional
    public void failed(PaymentStatusEvent event) { update(event.idOrden(), "CANCELADO"); }

    private void update(Long orderId, String status) {
        if (orderId == null) return;
        ordenRepository.findById(orderId).ifPresent(order -> {
            order.setEstado(status);
            ordenRepository.save(order);
        });
    }
}
