package com.GoTicket.Inventario.Messaging;

import com.GoTicket.Inventario.Model.Inventario;
import com.GoTicket.Inventario.Repository.InventarioRepository;
import com.GoTicket.Inventario.Model.BloqueoInventario;
import com.GoTicket.Inventario.Repository.BloqueoInventarioRepository;
import com.GoTicket.Inventario.Repository.EntradaRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderCreatedListener {
    private final InventarioRepository inventarioRepository;
    private final BloqueoInventarioRepository bloqueoRepository;
    private final EntradaRepository entradaRepository;

    @RabbitListener(queues = "${goticket.rabbit.order-queue}")
    @Transactional
    public void handle(OrderCreatedEvent event) {
        if (entradaRepository.findByIdOrden(event.idOrden()).isEmpty() == false) return;
        if (bloqueoRepository.findByIdOrden(event.idOrden()).isPresent()) return;
        inventarioRepository.findByIdEvento(event.idEvento()).ifPresent(inventory -> {
            int available = inventory.getCantidadDisponible() == null ? 0 : inventory.getCantidadDisponible();
            int requested = event.cantidad() == null ? 0 : event.cantidad();
            if (requested > 0 && available >= requested) {
                inventory.setCantidadDisponible(available - requested);
                inventory.setFechaActualizacion(java.time.LocalDateTime.now());
                inventarioRepository.save(inventory);
                bloqueoRepository.save(new BloqueoInventario(null, event.idOrden(), event.idEvento(), requested,
                        LocalDateTime.now().plusMinutes(10), false));
            }
        });
    }
}