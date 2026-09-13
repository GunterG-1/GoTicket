package com.GoTicket.Pagos.Messaging;

import com.GoTicket.Pagos.Model.Pago;
import com.GoTicket.Pagos.Repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreatedListener {
    private final PagoRepository pagoRepository;

    @RabbitListener(queues = "${goticket.rabbit.order-queue}")
    public void handle(OrderCreatedEvent event) {
        if (pagoRepository.findByIdOrden(event.idOrden()).stream().findAny().isPresent()) {
            return;
        }
        Pago payment = new Pago();
        payment.setIdOrden(event.idOrden());
        payment.setIdEvento(event.idEvento());
        payment.setIdUsuario(event.idUsuario());
        payment.setCantidad(event.cantidad());
        payment.setMonto(event.total());
        payment.setMetodoPago("PENDIENTE");
        payment.setEstado("PENDIENTE");
        pagoRepository.save(payment);
    }
}