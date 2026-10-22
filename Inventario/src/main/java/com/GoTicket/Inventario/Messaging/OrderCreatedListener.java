package com.GoTicket.Inventario.Messaging;

import com.GoTicket.Inventario.Model.Inventario;
import com.GoTicket.Inventario.Repository.InventarioRepository;
import com.GoTicket.Inventario.Model.BloqueoInventario;
import com.GoTicket.Inventario.Repository.BloqueoInventarioRepository;
import com.GoTicket.Inventario.Repository.EntradaRepository;
import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EstadoEntrada;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderCreatedListener {
    private final InventarioRepository inventarioRepository;
    private final BloqueoInventarioRepository bloqueoRepository;
    private final EntradaRepository entradaRepository;

    @Transactional
    public void handle(OrderCreatedEvent event) {
        if (event.idOrden() == null || event.idEvento() == null) {
            throw new IllegalArgumentException("La orden y el evento son obligatorios");
        }
        if (event.idEntradaReventa() != null) {
            Entrada resale = entradaRepository.findByIdForUpdate(event.idEntradaReventa())
                    .orElseThrow(() -> new IllegalStateException("Entrada publicada para reventa no encontrada"));
            if (EstadoEntrada.VENDIDO.equals(resale.getEstado())
                    && Objects.equals(resale.getIdOrden(), event.idOrden())) return;
            if (!EstadoEntrada.EN_REVENTA.equals(resale.getEstado())
                    || !Objects.equals(resale.getIdOrden(), event.idOrden())
                    || !Objects.equals(resale.getIdUsuarioReserva(), event.idUsuario())
                    || resale.getExpiraEn() == null || !resale.getExpiraEn().isAfter(LocalDateTime.now())
                    || event.expiraReserva() == null || !event.expiraReserva().isAfter(LocalDateTime.now())
                    || event.expiraReserva().isAfter(resale.getExpiraEn())
                    || !resale.getIdEvento().equals(event.idEvento())
                    || Objects.equals(resale.getIdUsuario(), event.idUsuario())
                    || event.cantidad() == null || event.cantidad() != 1
                    || event.total() == null || event.total().compareTo(resale.getPrecio()) != 0) {
                throw new IllegalStateException("La entrada ya no está disponible para reventa");
            }
            return;
        }

        if (bloqueoRepository.findByIdOrden(event.idOrden()).isPresent()) return;
        if (event.idReserva() == null || event.cantidad() == null || event.cantidad() <= 0) {
            throw new IllegalArgumentException("La reserva y la cantidad son obligatorias para la orden");
        }
        var reservedEntries = entradaRepository.findByIdEventoAndIdUsuarioAndIdReservaAndEstado(
                event.idEvento(), event.idUsuario(), event.idReserva(), EstadoEntrada.BLOQUEADO);
        if (reservedEntries.size() != event.cantidad()
                || reservedEntries.stream().anyMatch(entry -> entry.getExpiraEn() == null
                        || !entry.getExpiraEn().isAfter(LocalDateTime.now())
                        || !Objects.equals(event.sector(), entry.getSector()))) {
            throw new IllegalStateException("La orden no coincide con una reserva vigente");
        }
        LocalDateTime expiresAt = reservedEntries.stream().map(Entrada::getExpiraEn)
                .min(java.util.Comparator.naturalOrder()).orElseThrow();
        if (!expiresAt.equals(event.expiraReserva())) {
            throw new IllegalStateException("El vencimiento de la orden no coincide con la reserva");
        }
        BigDecimal reservedTotal = reservedEntries.stream().map(Entrada::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (event.total() == null || reservedTotal.compareTo(event.total()) != 0) {
            throw new IllegalStateException("El monto de la orden no coincide con los precios reservados");
        }
        reservedEntries.forEach(entry -> entry.setIdOrden(event.idOrden()));
        entradaRepository.saveAll(reservedEntries);

        Inventario inventory = inventarioRepository.findByIdEventoForUpdate(event.idEvento())
                .orElseThrow(() -> new IllegalStateException("Inventario no encontrado para la orden"));
        int available = inventory.getCantidadDisponible() == null ? 0 : inventory.getCantidadDisponible();
        int requested = event.cantidad();
        if (requested <= 0 || available < requested) {
            throw new IllegalStateException("No hay disponibilidad suficiente para confirmar la orden");
        }
        inventory.setCantidadDisponible(available - requested);
        inventory.setFechaActualizacion(LocalDateTime.now());
        inventarioRepository.save(inventory);
        bloqueoRepository.save(new BloqueoInventario(null, event.idOrden(), event.idEvento(), requested,
                expiresAt, false));
    }
}
