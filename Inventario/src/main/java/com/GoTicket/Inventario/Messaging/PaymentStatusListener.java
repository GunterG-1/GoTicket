package com.GoTicket.Inventario.Messaging;

import com.GoTicket.Inventario.Repository.InventarioRepository;
import com.GoTicket.Inventario.Repository.BloqueoInventarioRepository;
import com.GoTicket.Inventario.Repository.EntradaRepository;
import com.GoTicket.Inventario.Repository.TransferenciaEntradaRepository;
import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EstadoEntrada;
import com.GoTicket.Inventario.Service.TicketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;

@Component
@RequiredArgsConstructor
public class PaymentStatusListener {
    private final InventarioRepository inventarioRepository;
    private final BloqueoInventarioRepository bloqueoRepository;
    private final EntradaRepository entradaRepository;
    private final TransferenciaEntradaRepository transferenciaRepository;
    private final StringRedisTemplate redisTemplate;
    private final TicketNotificationService ticketNotificationService;

    @Transactional
    public void approved(PaymentStatusEvent event) {
        if (event.idEntradaReventa() != null) {
            Entrada resale = entradaRepository.findByIdForUpdate(event.idEntradaReventa())
                    .orElseThrow(() -> new IllegalStateException("Entrada de reventa no encontrada"));
            if (EstadoEntrada.VENDIDO.equals(resale.getEstado())
                    && event.idOrden().equals(resale.getIdOrden())) return;
            if (!EstadoEntrada.EN_REVENTA.equals(resale.getEstado())
                    || !java.util.Objects.equals(event.idOrden(), resale.getIdOrden())
                    || !java.util.Objects.equals(event.idUsuario(), resale.getIdUsuarioReserva())
                    || resale.getExpiraEn() == null
                    || !resale.getExpiraEn().isAfter(java.time.LocalDateTime.now())) {
                throw new IllegalStateException("La entrada de reventa ya no está reservada para esta orden");
            }
            Long previousOwner = resale.getIdUsuario();
            if (event.idUsuario() == null || event.idUsuario().equals(previousOwner)) {
                throw new IllegalStateException("El comprador de la reventa no es válido");
            }
            resale.setIdUsuario(event.idUsuario());
            resale.setEstado(EstadoEntrada.VENDIDO);
            if (resale.getCodigoAcceso() == null) {
                resale.setCodigoAcceso(java.util.UUID.randomUUID().toString());
            }
            resale.setFechaUso(null);
            resale.setFechaBloqueo(null);
            resale.setExpiraEn(null);
            resale.setIdUsuarioReserva(null);
            resale.setIdReserva(null);
            resale.setIdOrden(event.idOrden());
            entradaRepository.save(resale);
            ticketNotificationService.programar(event.idOrden(), event.idUsuario());
            java.math.BigDecimal commission = resale.getPrecio()
                    .multiply(java.math.BigDecimal.valueOf(0.05))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            transferenciaRepository.save(new com.GoTicket.Inventario.Model.TransferenciaEntrada(
                    null, resale.getIdEntrada(), previousOwner, event.idUsuario(), event.idOrden(),
                    java.time.LocalDateTime.now(), commission, resale.getPrecio().subtract(commission)));
            return;
        }
        java.util.List<Entrada> soldEntries = entradaRepository.findByIdOrden(event.idOrden());
        soldEntries.forEach(entry -> {
            entry.setEstado(EstadoEntrada.VENDIDO);
            if (entry.getCodigoAcceso() == null) {
                entry.setCodigoAcceso(java.util.UUID.randomUUID().toString());
            }
            entry.setFechaUso(null);
            entry.setFechaBloqueo(null);
            entry.setExpiraEn(null);
            entry.setIdReserva(null);
            redisTemplate.delete("goticket:entrada:" + entry.getIdEntrada());
            entradaRepository.save(entry);
        });
        bloqueoRepository.findByIdOrden(event.idOrden()).ifPresent(lock -> {
            lock.setResuelto(true);
            bloqueoRepository.save(lock);
        });
        if (!soldEntries.isEmpty()) {
            ticketNotificationService.programar(event.idOrden(), event.idUsuario());
        }
    }

    @Transactional
    public void failed(PaymentStatusEvent event) {
        if (event.idEntradaReventa() != null) {
            entradaRepository.findByIdForUpdate(event.idEntradaReventa()).ifPresent(resale -> {
                if (EstadoEntrada.EN_REVENTA.equals(resale.getEstado())
                        && event.idOrden().equals(resale.getIdOrden())
                        && java.util.Objects.equals(event.idUsuario(), resale.getIdUsuarioReserva())) {
                    resale.setIdOrden(null);
                    resale.setIdUsuarioReserva(null);
                    resale.setFechaBloqueo(null);
                    resale.setExpiraEn(null);
                    entradaRepository.save(resale);
                }
            });
            return;
        }
        if (event.idEvento() == null || event.cantidad() == null || event.cantidad() <= 0) return;
        entradaRepository.findByIdOrden(event.idOrden()).forEach(entry -> {
            entry.setEstado(EstadoEntrada.DISPONIBLE);
            entry.setIdUsuario(null);
            entry.setIdOrden(null);
            entry.setFechaBloqueo(null);
            entry.setExpiraEn(null);
            entry.setIdReserva(null);
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
