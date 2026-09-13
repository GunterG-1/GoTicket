package com.GoTicket.Inventario.Messaging;

import com.GoTicket.Inventario.Repository.InventarioRepository;
import com.GoTicket.Inventario.Repository.BloqueoInventarioRepository;
import com.GoTicket.Inventario.Repository.EntradaRepository;
import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EstadoEntrada;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;

@Component
@RequiredArgsConstructor
public class PaymentStatusListener {
    private final InventarioRepository inventarioRepository;
    private final BloqueoInventarioRepository bloqueoRepository;
    private final EntradaRepository entradaRepository;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(queues = "${goticket.rabbit.payment-approved-queue}")
    @Transactional
    public void approved(PaymentStatusEvent event) {
        entradaRepository.findByIdOrden(event.idOrden()).forEach(entry -> {
            entry.setEstado(EstadoEntrada.VENDIDO);
            entry.setFechaBloqueo(null);
            entry.setExpiraEn(null);
            redisTemplate.delete("goticket:entrada:" + entry.getIdEntrada());
            entradaRepository.save(entry);
        });
        bloqueoRepository.findByIdOrden(event.idOrden()).ifPresent(lock -> {
            lock.setResuelto(true);
            bloqueoRepository.save(lock);
        });
    }

    @RabbitListener(queues = "${goticket.rabbit.payment-failed-queue}")
    @Transactional
    public void failed(PaymentStatusEvent event) {
        if (event.idEvento() == null || event.cantidad() == null || event.cantidad() <= 0) return;
        entradaRepository.findByIdOrden(event.idOrden()).forEach(entry -> {
            entry.setEstado(EstadoEntrada.DISPONIBLE);
            entry.setIdUsuario(null);
            entry.setIdOrden(null);
            entry.setFechaBloqueo(null);
            entry.setExpiraEn(null);
            redisTemplate.delete("goticket:entrada:" + entry.getIdEntrada());
            entradaRepository.save(entry);
        });
        bloqueoRepository.findByIdOrden(event.idOrden()).ifPresent(lock -> {
            if (!lock.isResuelto()) {
                inventarioRepository.findByIdEvento(event.idEvento()).ifPresent(inventory -> {
                    int available = inventory.getCantidadDisponible() == null ? 0 : inventory.getCantidadDisponible();
                    int total = inventory.getCantidadTotal() == null ? Integer.MAX_VALUE : inventory.getCantidadTotal();
                    inventory.setCantidadDisponible(Math.min(total, available + event.cantidad()));
                    inventory.setFechaActualizacion(java.time.LocalDateTime.now());
                    inventarioRepository.save(inventory);
                });
                lock.setResuelto(true);
                bloqueoRepository.save(lock);
            }
        });
    }
}