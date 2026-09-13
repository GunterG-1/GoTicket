package com.GoTicket.Orden.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.GoTicket.Orden.Model.Orden;
import com.GoTicket.Orden.Repository.OrdenRepository;
import com.GoTicket.Orden.Messaging.OrderCreatedEvent;
import com.GoTicket.Orden.Model.OutboxEvent;
import com.GoTicket.Orden.Repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrdenServiceImpl implements OrdenService {

    private final OrdenRepository ordenRepository;
    private final RabbitTemplate rabbitTemplate;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Value("${goticket.rabbit.exchange}")
    private String exchangeName;

    @Value("${goticket.rabbit.order-routing-key}")
    private String routingKey;

    @Override
    public List<Orden> listarOrdenes() {
        return ordenRepository.findAll();
    }

    @Override
    public Optional<Orden> buscarOrdenPorId(Long id) {
        return ordenRepository.findById(id);
    }

    @Override
    public List<Orden> buscarOrdenesPorUsuario(Long idUsuario) {
        return ordenRepository.findByIdUsuario(idUsuario);
    }

    @Override
    @Transactional
    public Orden guardarOrden(Orden orden) {
        if (orden.getCantidad() == null || orden.getCantidad() <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a cero");
        }

        if (orden.getPrecioUnitario() == null || orden.getPrecioUnitario().doubleValue() <= 0) {
            throw new RuntimeException("El precio unitario debe ser mayor a cero");
        }

        orden.setEstado("PENDIENTE");

        orden.setTotal(orden.getPrecioUnitario().multiply(java.math.BigDecimal.valueOf(orden.getCantidad())));
        orden.setFechaCreacion(LocalDateTime.now());
        orden.setFechaActualizacion(LocalDateTime.now());

        Orden savedOrder = ordenRepository.save(orden);
        try {
            OrderCreatedEvent event = new OrderCreatedEvent(savedOrder.getIdOrden(), savedOrder.getIdEvento(), savedOrder.getIdUsuario(),
                savedOrder.getCantidad(), savedOrder.getTotal(), savedOrder.getSector());
            outboxRepository.save(new OutboxEvent(null, "ORDEN_CREADA", objectMapper.writeValueAsString(event),
                false, 0, LocalDateTime.now(), null));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo registrar el evento de orden", exception);
        }
        return savedOrder;
    }

    @Override
    @Transactional
    public Orden actualizarOrden(Long id, Orden orden) {
        Orden ordenExistente = ordenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada con id: " + id));

        ordenExistente.setIdEvento(orden.getIdEvento());
        ordenExistente.setIdUsuario(orden.getIdUsuario());
        ordenExistente.setCantidad(orden.getCantidad());
        ordenExistente.setPrecioUnitario(orden.getPrecioUnitario());
        ordenExistente.setTotal(orden.getPrecioUnitario().multiply(java.math.BigDecimal.valueOf(orden.getCantidad())));
        ordenExistente.setEstado(orden.getEstado());
        ordenExistente.setFechaActualizacion(LocalDateTime.now());

        return ordenRepository.save(ordenExistente);
    }

    @Override
    @Transactional
    public void eliminarOrden(Long id) {
        if (!ordenRepository.existsById(id)) {
            throw new RuntimeException("Orden no encontrada con id: " + id);
        }
        ordenRepository.deleteById(id);
    }
}
