package com.GoTicket.Orden.Messaging;

import com.GoTicket.Orden.Model.Orden;
import com.GoTicket.Orden.Repository.OrdenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PaymentStatusListener {
    private final OrdenRepository ordenRepository;

    @RabbitListener(queues = "${goticket.rabbit.payment-approved-queue}")
    @Transactional
    public void approved(PaymentStatusEvent event) { update(event.idOrden(), "COMPLETADO"); }

    @RabbitListener(queues = "${goticket.rabbit.payment-failed-queue}")
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