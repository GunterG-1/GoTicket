package com.GoTicket.Inventario.Messaging;

import com.GoTicket.Inventario.Model.Inventario;
import com.GoTicket.Inventario.Repository.InventarioRepository;
import com.GoTicket.Inventario.Model.BloqueoInventario;
import com.GoTicket.Inventario.Repository.BloqueoInventarioRepository;
import com.GoTicket.Inventario.Repository.EntradaRepository;
import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EstadoEntrada;
import java.time.LocalDateTime;
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
            if (resale.getIdOrden() != null && resale.getIdOrden().equals(event.idOrden())) return;
            if (!EstadoEntrada.EN_REVENTA.equals(resale.getEstado()) || resale.getIdOrden() != null
                    || !resale.getIdEvento().equals(event.idEvento())
                    || Objects.equals(resale.getIdUsuario(), event.idUsuario())) {
                throw new IllegalStateException("La entrada ya no está disponible para reventa");
            }
            resale.setIdOrden(event.idOrden());
            entradaRepository.save(resale);
            return;
        }

        Inventario inventory = inventarioRepository.findByIdEventoForUpdate(event.idEvento())
                .orElseThrow(() -> new IllegalStateException("Inventario no encontrado para la orden"));
        if (bloqueoRepository.findByIdOrden(event.idOrden()).isPresent()) return;
        int available = inventory.getCantidadDisponible() == null ? 0 : inventory.getCantidadDisponible();
        int requested = event.cantidad() == null ? 0 : event.cantidad();
        if (requested <= 0 || available < requested) {
            throw new IllegalStateException("No hay disponibilidad suficiente para confirmar la orden");
        }
        inventory.setCantidadDisponible(available - requested);
        inventory.setFechaActualizacion(LocalDateTime.now());
        inventarioRepository.save(inventory);
        bloqueoRepository.save(new BloqueoInventario(null, event.idOrden(), event.idEvento(), requested,
                LocalDateTime.now().plusMinutes(10), false));
    }
}
