package com.GoTicket.Orden.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.GoTicket.Orden.Model.Orden;
import com.GoTicket.Orden.Model.CheckoutRequest;
import com.GoTicket.Orden.Repository.OrdenRepository;
import com.GoTicket.Orden.Messaging.OrderCreatedEvent;
import com.GoTicket.Orden.Model.OutboxEvent;
import com.GoTicket.Orden.Repository.OutboxEventRepository;
import com.GoTicket.Orden.Client.InventarioReservaClient;
import com.GoTicket.Orden.Client.ReservaDetalles;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrdenServiceImpl implements OrdenService {

    private final OrdenRepository ordenRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final InventarioReservaClient inventarioReservaClient;

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
        return crearOrden(orden, false);
    }

    private Orden crearOrden(Orden orden, boolean desdeCheckout) {
        if (orden.getCantidad() == null || orden.getCantidad() <= 0 || orden.getCantidad() > 10) {
            throw new IllegalArgumentException("La cantidad debe estar entre 1 y 10");
        }
        if (orden.getIdEvento() == null || orden.getIdUsuario() == null) {
            throw new IllegalArgumentException("El evento y el usuario son obligatorios");
        }
        if (orden.getIdEntradaReventa() != null && orden.getCantidad() != 1) {
            throw new IllegalArgumentException("Una entrada publicada para reventa se vende individualmente");
        }
        if (orden.getIdEntradaReventa() != null && !desdeCheckout) {
            throw new IllegalArgumentException("Las entradas de reventa deben comprarse mediante checkout");
        }

        if (orden.getIdEntradaReventa() == null) {
            if (orden.getIdReserva() == null || orden.getIdReserva().isBlank()) {
                throw new IllegalArgumentException("La reserva es obligatoria para crear una orden de entradas normales");
            }
            try {
                UUID.fromString(orden.getIdReserva());
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("El identificador de reserva no es válido", exception);
            }
            ReservaDetalles reservation = inventarioReservaClient.buscarReserva(orden.getIdReserva());
            if (reservation == null || !Objects.equals(reservation.idEvento(), orden.getIdEvento())
                    || !Objects.equals(reservation.idUsuario(), orden.getIdUsuario())
                    || !Objects.equals(reservation.cantidad(), orden.getCantidad())
                    || !Objects.equals(reservation.sector(), orden.getSector())
                    || reservation.expiraEn() == null || !reservation.expiraEn().isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException("La reserva no corresponde a la orden o ya expiró");
            }
            orden.setPrecioUnitario(reservation.precioUnitario());
            orden.setExpiraReserva(reservation.expiraEn());
        } else {
            if (orden.getCantidad() != 1) {
                throw new IllegalArgumentException("Una entrada publicada para reventa se vende individualmente");
            }
            com.GoTicket.Orden.Client.ReventaDetalles listing =
                    inventarioReservaClient.buscarEntradaEnReventa(orden.getIdEntradaReventa());
            if (listing == null || !Objects.equals(listing.idEvento(), orden.getIdEvento())
                    || Objects.equals(listing.idUsuarioVendedor(), orden.getIdUsuario())
                    || listing.precio() == null || listing.precio().signum() <= 0) {
                throw new IllegalArgumentException("La entrada no está disponible para este comprador");
            }
            orden.setPrecioUnitario(listing.precio());
            orden.setSector(listing.sector());
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
                savedOrder.getCantidad(), savedOrder.getTotal(), savedOrder.getSector(), savedOrder.getIdEntradaReventa(),
                savedOrder.getIdReserva(), savedOrder.getExpiraReserva());
            outboxRepository.save(new OutboxEvent(null, "ORDEN_CREADA", objectMapper.writeValueAsString(event),
                false, 0, LocalDateTime.now(), null));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo registrar el evento de orden", exception);
        }
        return savedOrder;
    }

    @Override
    @Transactional
    public Orden checkout(CheckoutRequest request) {
        if (request == null || request.idEvento() == null || request.idUsuario() == null
                || request.cantidad() == null || request.cantidad() <= 0 || request.cantidad() > 10) {
            throw new IllegalArgumentException("Evento, usuario y cantidad entre 1 y 10 son obligatorios");
        }
        String idReserva = UUID.randomUUID().toString();
        String sector = request.sector() == null || request.sector().isBlank()
                ? "General" : request.sector().trim();
        Long idOrden = null;
        boolean resaleHoldAttempted = false;
        try {
            Orden order = new Orden();
            order.setIdEvento(request.idEvento());
            order.setIdUsuario(request.idUsuario());
            order.setCantidad(request.cantidad());
            order.setSector(sector);
            if (request.idEntradaReventa() != null) {
                if (request.cantidad() != 1) {
                    throw new IllegalArgumentException("Una entrada publicada para reventa se vende individualmente");
                }
                order.setIdEntradaReventa(request.idEntradaReventa());
                order.setExpiraReserva(LocalDateTime.now().plusMinutes(5));
                Orden savedOrder = crearOrden(order, true);
                idOrden = savedOrder.getIdOrden();
                resaleHoldAttempted = true;
                inventarioReservaClient.reservarReventa(
                        request.idEntradaReventa(), request.idUsuario(), idOrden);
                return savedOrder;
            }

            inventarioReservaClient.reservarEntradas(new com.GoTicket.Orden.Client.ReservarEntradasRequest(
                    request.idEvento(), request.idUsuario(), sector, request.cantidad(), idReserva));
            order.setIdReserva(idReserva);
            Orden savedOrder = crearOrden(order, true);
            idOrden = savedOrder.getIdOrden();
            inventarioReservaClient.confirmarReserva(new com.GoTicket.Orden.Client.ConfirmarReservaRequest(
                    request.idEvento(), request.idUsuario(), idOrden, idReserva));
            return savedOrder;
        } catch (RuntimeException checkoutFailure) {
            try {
                if (request.idEntradaReventa() != null) {
                    if (resaleHoldAttempted && idOrden != null) {
                        inventarioReservaClient.cancelarReservaReventa(
                                request.idEntradaReventa(), request.idUsuario(), idOrden);
                    }
                } else {
                    inventarioReservaClient.cancelarReserva(idReserva, request.idUsuario(), idOrden);
                }
            } catch (RuntimeException compensationFailure) {
                checkoutFailure.addSuppressed(compensationFailure);
                throw new IllegalStateException(
                        "El checkout falló y no se pudo liberar automáticamente la reserva", checkoutFailure);
            }
            throw checkoutFailure;
        }
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
