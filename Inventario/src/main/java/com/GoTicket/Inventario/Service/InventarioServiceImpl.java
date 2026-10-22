package com.GoTicket.Inventario.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.GoTicket.Inventario.Model.Inventario;
import com.GoTicket.Inventario.Model.BloqueoInventario;
import com.GoTicket.Inventario.Model.EntradaUsuarioResponse;
import com.GoTicket.Inventario.Model.ReservaDetalles;
import com.GoTicket.Inventario.Model.ReventaDetalles;
import com.GoTicket.Inventario.Repository.InventarioRepository;
import com.GoTicket.Inventario.Repository.EntradaRepository;
import com.GoTicket.Inventario.Model.ConfirmarReservaRequest;
import com.GoTicket.Inventario.Model.CancelarReservaRequest;
import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EntradaReservaRequest;
import com.GoTicket.Inventario.Model.EstadoEntrada;
import com.GoTicket.Inventario.Model.PublicarReventaRequest;
import com.GoTicket.Inventario.Model.TransferirEntradaRequest;
import com.GoTicket.Inventario.Model.TransferenciaEntrada;
import com.GoTicket.Inventario.Model.SectorDisponibilidad;
import com.GoTicket.Inventario.Repository.TransferenciaEntradaRepository;
import com.GoTicket.Inventario.Client.CatalogoEvento;
import com.GoTicket.Inventario.Client.CatalogoEventoClient;
import com.GoTicket.Inventario.Client.SectorEvento;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import com.GoTicket.Inventario.Repository.BloqueoInventarioRepository;
import com.GoTicket.Inventario.Messaging.PaymentStatusEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class InventarioServiceImpl implements InventarioService {

    private final InventarioRepository inventarioRepository;
    private final EntradaRepository entradaRepository;
    private final StringRedisTemplate redisTemplate;
    private final TransferenciaEntradaRepository transferenciaRepository;
    private final CatalogoEventoClient catalogoEventoClient;
    private final BloqueoInventarioRepository bloqueoRepository;
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    @Value("${goticket.aws.sns.topic-arn}")
    private String topicArn;
    @Value("${goticket.aws.event.failed:pago.fallido}")
    private String paymentFailedKey;

    @Value("${goticket.reservation.minutes:5}")
    private long reservationMinutes = 5;


    @Override
    public List<Inventario> listarInventarios() {
        return inventarioRepository.findAll();
    }

    @Override
    public Optional<Inventario> buscarInventarioPorId(Long id) {
        return inventarioRepository.findById(id);
    }

    @Override
    public Optional<Inventario> buscarInventarioPorEvento(Long idEvento) {
        return inventarioRepository.findByIdEvento(idEvento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectorDisponibilidad> disponibilidadPorSector(Long idEvento) {
        Inventario inventory = inventarioRepository.findByIdEvento(idEvento)
                .orElseThrow(() -> new IllegalArgumentException("Inventario no encontrado para el evento: " + idEvento));
        List<SectorEvento> sectors = catalogoEventoClient.listarSectores(idEvento);
        if (sectors.isEmpty()) {
            throw new IllegalStateException("El evento aún no tiene sectores configurados");
        }
        Map<String, Long> committed = entradaRepository.findByIdEvento(idEvento).stream()
                .filter(entry -> entry.getEstado() != EstadoEntrada.DISPONIBLE)
                .collect(Collectors.groupingBy(Entrada::getSector, Collectors.counting()));
        return sectors.stream()
                .map(sector -> new SectorDisponibilidad(sector.nombre(), sector.capacidad(),
                        Math.max(0, sector.capacidad() - committed.getOrDefault(sector.nombre(), 0L).intValue()),
                        sector.precio(), sector.modalidad(), sector.color(),
                        sector.posicionX(), sector.posicionY()))
                .toList();
    }

    @Override
    @Transactional
    public Inventario guardarInventario(Inventario inventario) {
        if (inventario.getIdEvento() == null) {
            throw new IllegalArgumentException("El evento es obligatorio para crear inventario");
        }
        if (inventario.getCantidadDisponible() == null) {
            inventario.setCantidadDisponible(0);
        }
        if (inventario.getCantidadTotal() == null) {
            inventario.setCantidadTotal(0);
        }
        if (inventario.getCantidadTotal() <= 0 || inventario.getCantidadDisponible() < 0
                || inventario.getCantidadDisponible() > inventario.getCantidadTotal()) {
            throw new IllegalArgumentException("Las cantidades de inventario no son válidas");
        }
        if (inventario.getActivo() == null) {
            inventario.setActivo(true);
        }
        inventario.setFechaActualizacion(LocalDateTime.now());
        inventarioRepository.crearSiNoExiste(inventario.getIdEvento(), inventario.getCantidadDisponible(),
                inventario.getCantidadTotal(), inventario.getActivo(), inventario.getFechaActualizacion());
        return inventarioRepository.findByIdEvento(inventario.getIdEvento())
                .orElseThrow(() -> new IllegalStateException("No se pudo crear ni recuperar el inventario del evento"));
    }

    @Override
    @Transactional
    public Inventario actualizarInventario(Long id, Inventario inventario) {
        Inventario inventarioExistente = inventarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventario no encontrado con id: " + id));

        int nuevaCantidadTotal = inventario.getCantidadTotal() == null ? 0 : inventario.getCantidadTotal();
        if (nuevaCantidadTotal <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor que cero");
        }
        int cantidadTotalAnterior = inventarioExistente.getCantidadTotal() == null
                ? 0 : inventarioExistente.getCantidadTotal();
        int cantidadDisponibleAnterior = inventarioExistente.getCantidadDisponible() == null
                ? 0 : inventarioExistente.getCantidadDisponible();
        long entradasComprometidas = entradaRepository.findByIdEvento(inventarioExistente.getIdEvento()).stream()
                .filter(entrada -> entrada.getEstado() != EstadoEntrada.DISPONIBLE
                        && entrada.getEstado() != EstadoEntrada.EN_REVENTA)
                .count();
        if (nuevaCantidadTotal < entradasComprometidas) {
            throw new IllegalArgumentException(
                    "La capacidad no puede ser menor que las entradas comprometidas (" + entradasComprometidas + ")");
        }
        int diferencia = nuevaCantidadTotal - cantidadTotalAnterior;
        inventarioExistente.setCantidadDisponible(Math.max(0, cantidadDisponibleAnterior + diferencia));
        inventarioExistente.setCantidadTotal(nuevaCantidadTotal);
        inventarioExistente.setActivo(inventario.getActivo());
        inventarioExistente.setFechaActualizacion(LocalDateTime.now());

        return inventarioRepository.save(inventarioExistente);
    }

    @Override
    @Transactional
    public Inventario actualizarInventarioPorEvento(Long idEvento, Inventario inventario) {
        Inventario existente = inventarioRepository.findByIdEvento(idEvento)
                .orElseThrow(() -> new RuntimeException("Inventario no encontrado para el evento: " + idEvento));
        return actualizarInventario(existente.getIdInventario(), inventario);
    }

    @Override
    @Transactional
    public void eliminarInventario(Long id) {
        if (!inventarioRepository.existsById(id)) {
            throw new RuntimeException("Inventario no encontrado con id: " + id);
        }
        inventarioRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void eliminarInventarioPorEvento(Long idEvento) {
        Inventario inventario = inventarioRepository.findByIdEvento(idEvento)
                .orElseThrow(() -> new RuntimeException("Inventario no encontrado para el evento: " + idEvento));
        boolean tieneEntradasComprometidas = entradaRepository.findByIdEvento(idEvento).stream()
                .anyMatch(entrada -> entrada.getEstado() != EstadoEntrada.DISPONIBLE);
        if (tieneEntradasComprometidas) {
            throw new IllegalStateException("No se puede eliminar el evento porque tiene entradas comprometidas");
        }
        entradaRepository.deleteAll(entradaRepository.findByIdEvento(idEvento));
        inventarioRepository.delete(inventario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entrada> listarEntradas(Long idEvento) {
        return entradaRepository.findByIdEvento(idEvento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntradaUsuarioResponse> listarEntradasDeUsuario(Long idUsuario) {
        return entradaRepository.findByIdUsuario(idUsuario).stream()
                .map(entry -> new EntradaUsuarioResponse(entry.getIdEntrada(), entry.getIdEvento(),
                        entry.getSector(), entry.getNumeroAsiento(), entry.getPrecio(), entry.getEstado(),
                        entry.getFechaUso(), entry.getCodigoAcceso()))
                .toList();
    }

    @Override
    @Transactional
    public Entrada validarAcceso(String codigoAcceso) {
        if (codigoAcceso == null || codigoAcceso.isBlank()) {
            throw new IllegalArgumentException("El código de acceso es obligatorio");
        }
        Entrada entry = entradaRepository.findByCodigoAccesoForUpdate(codigoAcceso.trim())
                .orElseThrow(() -> new IllegalArgumentException("Código de acceso inválido"));
        if (entry.getEstado() != EstadoEntrada.VENDIDO) {
            throw new IllegalStateException("La entrada no está habilitada para acceso");
        }
        entry.setEstado(EstadoEntrada.USADA);
        entry.setFechaUso(LocalDateTime.now());
        entry.setCodigoAcceso(null);
        return entradaRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entrada> listarEntradasEnReventa() {
        return entradaRepository.findByEstadoAndIdOrdenIsNull(EstadoEntrada.EN_REVENTA);
    }

    @Override
    @Transactional(readOnly = true)
    public ReventaDetalles buscarEntradaEnReventa(Long idEntrada) {
        Entrada entry = entradaRepository.findById(idEntrada)
                .orElseThrow(() -> new IllegalArgumentException("Entrada no encontrada"));
        if (entry.getEstado() != EstadoEntrada.EN_REVENTA || entry.getIdOrden() != null
                || entry.getPrecio() == null || entry.getPrecio().signum() <= 0) {
            throw new IllegalStateException("La entrada ya no está disponible para reventa");
        }
        return new ReventaDetalles(entry.getIdEntrada(), entry.getIdEvento(), entry.getIdUsuario(),
                entry.getSector(), entry.getPrecio());
    }

    @Override
    @Transactional
    public List<Entrada> reservarEntradas(EntradaReservaRequest request) {
        if (request.idEvento() == null || request.idUsuario() == null || request.cantidad() == null
                || request.cantidad() <= 0 || request.cantidad() > 10) {
            throw new IllegalArgumentException("Evento, usuario y cantidad entre 1 y 10 son obligatorios");
        }
        validarIdReserva(request.idReserva());
        String sector = request.sector() == null || request.sector().isBlank() ? "General" : request.sector().trim();
        CatalogoEvento event = asegurarEventoComprable(request.idEvento());
        asegurarEntradasFisicas(request.idEvento(), sector, request.cantidad());
        List<Entrada> available = entradaRepository.findByIdEventoAndSectorAndEstado(request.idEvento(), sector, EstadoEntrada.DISPONIBLE);
        if (available.size() < request.cantidad()) {
            throw new IllegalStateException("No hay entradas disponibles para el sector solicitado");
        }

        List<Entrada> reserved = new ArrayList<>();
        LocalDateTime expires = LocalDateTime.now().plusMinutes(reservationMinutes);
        for (Entrada entry : available) {
            if (reserved.size() == request.cantidad()) break;
            String lockKey = "goticket:entrada:" + entry.getIdEntrada();
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, String.valueOf(request.idUsuario()), java.time.Duration.ofMinutes(reservationMinutes));
            if (!Boolean.TRUE.equals(locked)) continue;
            entry.setEstado(EstadoEntrada.BLOQUEADO);
            entry.setIdUsuario(request.idUsuario());
            entry.setIdReserva(request.idReserva());
            entry.setFechaBloqueo(LocalDateTime.now());
            entry.setExpiraEn(expires);
            reserved.add(entry);
        }
        if (reserved.size() != request.cantidad()) {
            reserved.forEach(entry -> redisTemplate.delete("goticket:entrada:" + entry.getIdEntrada()));
            throw new IllegalStateException("Las entradas fueron reservadas por otro usuario");
        }
        return entradaRepository.saveAll(reserved);
    }

    private CatalogoEvento asegurarEventoComprable(Long idEvento) {
        CatalogoEvento event = catalogoEventoClient.buscarEvento(idEvento)
                .orElseThrow(() -> new IllegalStateException("No se pudo obtener el evento desde Catalogo-Evento"));
        if (!Boolean.TRUE.equals(event.activo())) {
            throw new IllegalStateException("El evento no está activo para la venta");
        }
        if (event.fechaInicio() == null || !event.fechaInicio().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("La venta para este evento ya no está disponible");
        }
        return event;
    }

    private void validarIdReserva(String idReserva) {
        if (idReserva == null || idReserva.isBlank()) {
            throw new IllegalArgumentException("El identificador de reserva es obligatorio");
        }
        try {
            UUID.fromString(idReserva);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El identificador de reserva no es válido", exception);
        }
    }

    private void asegurarEntradasFisicas(Long idEvento, String sector, int requested) {
        List<SectorEvento> configuredSectors = catalogoEventoClient.listarSectores(idEvento);
        if (configuredSectors.isEmpty()) {
            throw new IllegalStateException("El evento aún no tiene sectores configurados");
        }
        Inventario inventory = inventarioRepository.findByIdEventoForUpdate(idEvento).orElseGet(() -> {
            int capacity = configuredSectors.stream().mapToInt(SectorEvento::capacidad).sum();
            Inventario created = new Inventario();
            created.setIdEvento(idEvento);
            created.setCantidadTotal(capacity);
            created.setCantidadDisponible(capacity);
            created.setActivo(true);
            created.setFechaActualizacion(LocalDateTime.now());
            return inventarioRepository.save(created);
        });
        int availableInAggregate = inventory.getCantidadDisponible() == null ? 0 : inventory.getCantidadDisponible();
        if (availableInAggregate < requested) {
            throw new IllegalStateException("No hay entradas disponibles para el evento solicitado");
        }

        SectorEvento configuredSector = configuredSectors.stream()
                .filter(item -> item.nombre().equals(sector))
                .findFirst()
                .orElse(null);
        if (configuredSector == null) {
            throw new IllegalArgumentException("El sector solicitado no corresponde a este evento");
        }
        int sectorCapacity = configuredSector.capacidad();
        List<Entrada> allEntries = entradaRepository.findByIdEvento(idEvento);
        long committedInSector = allEntries.stream()
                .filter(entry -> sector.equals(entry.getSector()) && entry.getEstado() != EstadoEntrada.DISPONIBLE)
                .count();
        List<Entrada> existingAvailable = entradaRepository.findByIdEventoAndSectorAndEstado(
                idEvento, sector, EstadoEntrada.DISPONIBLE);
        if (committedInSector + requested > sectorCapacity) {
            throw new IllegalStateException("No hay entradas disponibles para el sector solicitado");
        }
        int missing = requested - existingAvailable.size();
        if (missing <= 0) return;

        int existingCount = (int) allEntries.stream().filter(entry -> sector.equals(entry.getSector())).count();
        int toCreate = Math.min(missing,
                Math.max(0, sectorCapacity - (int) committedInSector - existingAvailable.size()));
        if (toCreate < missing) {
            throw new IllegalStateException("No hay entradas fÃ­sicas suficientes para el evento solicitado");
        }

        List<Entrada> newEntries = new ArrayList<>();
        BigDecimal ticketPrice = configuredSector.precio();
        if (ticketPrice == null || ticketPrice.signum() < 0) {
            throw new IllegalStateException("El sector no tiene un precio válido configurado");
        }
        for (int index = 1; index <= toCreate; index++) {
            newEntries.add(new Entrada(null, idEvento, sector, String.valueOf(existingCount + index),
                        ticketPrice, ticketPrice,
                        EstadoEntrada.DISPONIBLE, null, null, null, null, null, null, null, null));
        }
        entradaRepository.saveAll(newEntries);
    }

    @Override
    @Transactional
    public void confirmarReserva(ConfirmarReservaRequest request) {
        if (request.idEvento() == null || request.idUsuario() == null || request.idOrden() == null
                || request.idReserva() == null) {
            throw new IllegalArgumentException("Evento, usuario, reserva y orden son obligatorios");
        }
        validarIdReserva(request.idReserva());
        List<Entrada> entries = entradaRepository.findByIdEventoAndIdUsuarioAndIdReservaAndEstado(
                request.idEvento(), request.idUsuario(), request.idReserva(), EstadoEntrada.BLOQUEADO);
        if (entries.isEmpty()) {
            throw new IllegalStateException("La reserva no tiene entradas asociadas");
        }
        LocalDateTime now = LocalDateTime.now();
        if (entries.stream().anyMatch(entry -> entry.getExpiraEn() == null || !entry.getExpiraEn().isAfter(now))) {
            throw new IllegalStateException("La reserva expiró y ya no puede confirmarse");
        }
        Optional<BloqueoInventario> existingLock = bloqueoRepository.findByIdOrden(request.idOrden());
        if (existingLock.isPresent() && (!existingLock.get().getIdEvento().equals(request.idEvento())
                || existingLock.get().getCantidad() != entries.size())) {
            throw new IllegalStateException("La orden ya está asociada a otra reserva");
        }
        entries.forEach(entry -> entry.setIdOrden(request.idOrden()));
        entradaRepository.saveAll(entries);
        Inventario inventory = inventarioRepository.findByIdEventoForUpdate(request.idEvento())
                .orElseThrow(() -> new IllegalStateException("Inventario no encontrado para el evento"));
        if (existingLock.isEmpty()) {
            int available = inventory.getCantidadDisponible() == null ? 0 : inventory.getCantidadDisponible();
            if (available < entries.size()) {
                throw new IllegalStateException("No hay disponibilidad suficiente para confirmar la reserva");
            }
            inventory.setCantidadDisponible(available - entries.size());
            inventory.setFechaActualizacion(LocalDateTime.now());
            inventarioRepository.save(inventory);
            bloqueoRepository.save(new com.GoTicket.Inventario.Model.BloqueoInventario(
                    null, request.idOrden(), request.idEvento(), entries.size(),
                    entries.stream().map(Entrada::getExpiraEn).min(Comparator.naturalOrder()).orElseThrow(), false));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaDetalles buscarReserva(String idReserva) {
        validarIdReserva(idReserva);
        List<Entrada> entries = entradaRepository.findByIdReservaAndEstado(idReserva, EstadoEntrada.BLOQUEADO);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("La reserva no existe o ya expiró");
        }
        Entrada first = entries.get(0);
        LocalDateTime expiresAt = entries.stream().map(Entrada::getExpiraEn)
                .filter(java.util.Objects::nonNull).min(Comparator.naturalOrder())
                .orElseThrow(() -> new IllegalStateException("La reserva no tiene fecha de expiración"));
        if (!expiresAt.isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("La reserva ya expiró");
        }
        if (entries.stream().anyMatch(entry -> !first.getIdEvento().equals(entry.getIdEvento())
                || !first.getIdUsuario().equals(entry.getIdUsuario())
                || !java.util.Objects.equals(first.getSector(), entry.getSector())
                || entry.getPrecio().compareTo(first.getPrecio()) != 0)) {
            throw new IllegalStateException("La reserva contiene entradas inconsistentes");
        }
        return new ReservaDetalles(first.getIdEvento(), first.getIdUsuario(), first.getSector(), entries.size(),
                first.getPrecio(), expiresAt);
    }

    @Override
    @Transactional
    public void cancelarReserva(CancelarReservaRequest request) {
        if (request == null || request.idUsuario() == null) {
            throw new IllegalArgumentException("El usuario es obligatorio para cancelar la reserva");
        }
        validarIdReserva(request.idReserva());
        List<Entrada> entries = entradaRepository.findByIdReservaAndEstadoForUpdate(
                request.idReserva(), EstadoEntrada.BLOQUEADO);
        if (entries.isEmpty()) return;
        if (entries.stream().anyMatch(entry -> !request.idUsuario().equals(entry.getIdUsuario())
                || (entry.getIdOrden() != null && !entry.getIdOrden().equals(request.idOrden())))) {
            throw new IllegalStateException("La reserva no pertenece al usuario o la orden indicada");
        }

        entries.forEach(entry -> {
            entry.setEstado(EstadoEntrada.DISPONIBLE);
            entry.setIdUsuario(null);
            entry.setIdOrden(null);
            entry.setIdReserva(null);
            entry.setFechaBloqueo(null);
            entry.setExpiraEn(null);
            redisTemplate.delete("goticket:entrada:" + entry.getIdEntrada());
        });
        entradaRepository.saveAll(entries);

        if (request.idOrden() == null) return;
        bloqueoRepository.findByIdOrdenForUpdate(request.idOrden()).ifPresent(lock -> {
            if (!lock.isResuelto()) {
                inventarioRepository.findByIdEventoForUpdate(lock.getIdEvento()).ifPresent(inventory -> {
                    int available = inventory.getCantidadDisponible() == null ? 0 : inventory.getCantidadDisponible();
                    int total = inventory.getCantidadTotal() == null ? Integer.MAX_VALUE : inventory.getCantidadTotal();
                    inventory.setCantidadDisponible(Math.min(total, available + lock.getCantidad()));
                    inventory.setFechaActualizacion(LocalDateTime.now());
                    inventarioRepository.save(inventory);
                });
                lock.setResuelto(true);
                bloqueoRepository.save(lock);
            }
        });
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void liberarEntradasExpiradas() {
        Map<Long, Integer> expiredOrders = new LinkedHashMap<>();
        Map<Long, Entrada> orderEntries = new LinkedHashMap<>();
        List<PaymentStatusEvent> expiredResalePayments = new ArrayList<>();
        entradaRepository.findByEstadoAndExpiraEnBefore(EstadoEntrada.BLOQUEADO, LocalDateTime.now()).forEach(entry -> {
            if (entry.getIdOrden() != null) {
                expiredOrders.merge(entry.getIdOrden(), 1, Integer::sum);
                Entrada eventEntry = new Entrada();
                eventEntry.setIdEvento(entry.getIdEvento());
                eventEntry.setIdUsuario(entry.getIdUsuario());
                orderEntries.putIfAbsent(entry.getIdOrden(), eventEntry);
            }
            entry.setEstado(EstadoEntrada.DISPONIBLE);
            entry.setIdUsuario(null);
            entry.setIdOrden(null);
            entry.setIdReserva(null);
            entry.setFechaBloqueo(null);
            entry.setExpiraEn(null);
            redisTemplate.delete("goticket:entrada:" + entry.getIdEntrada());
            entradaRepository.save(entry);
        });
        entradaRepository.findByEstadoAndExpiraEnBefore(EstadoEntrada.EN_REVENTA, LocalDateTime.now()).forEach(entry -> {
            if (entry.getIdOrden() != null) {
                expiredResalePayments.add(new PaymentStatusEvent(null, entry.getIdOrden(), entry.getIdEvento(),
                        entry.getIdUsuarioReserva(), 1, entry.getPrecio(), "RECHAZADO", entry.getIdEntrada()));
                entry.setIdOrden(null);
                entry.setIdUsuarioReserva(null);
                entry.setFechaBloqueo(null);
                entry.setExpiraEn(null);
                entradaRepository.save(entry);
            }
        });
        expiredOrders.forEach((orderId, quantity) -> {
            Entrada entry = orderEntries.get(orderId);
            try {
                PaymentStatusEvent event = new PaymentStatusEvent(null, orderId, entry.getIdEvento(),
                        entry.getIdUsuario(), quantity, null, "RECHAZADO", null);
                snsClient.publish(PublishRequest.builder().topicArn(topicArn)
                        .message(objectMapper.writeValueAsString(event))
                        .messageAttributes(Map.of("eventType",
                                software.amazon.awssdk.services.sns.model.MessageAttributeValue.builder()
                                        .dataType("String").stringValue(paymentFailedKey).build()))
                        .build());
            } catch (Exception exception) {
                throw new IllegalStateException("No se pudo publicar el evento de pago fallido", exception);
            }
        });
        expiredResalePayments.forEach(event -> {
            try {
                snsClient.publish(PublishRequest.builder().topicArn(topicArn)
                        .message(objectMapper.writeValueAsString(event))
                        .messageAttributes(Map.of("eventType",
                                software.amazon.awssdk.services.sns.model.MessageAttributeValue.builder()
                                        .dataType("String").stringValue(paymentFailedKey).build()))
                        .build());
            } catch (Exception exception) {
                throw new IllegalStateException("No se pudo publicar la expiración del pago de reventa", exception);
            }
        });
    }

    @Override
    @Transactional
    public Entrada publicarReventa(Long idEntrada, PublicarReventaRequest request) {
        if (request.idUsuario() == null || request.precio() == null || request.precio().signum() <= 0) {
            throw new IllegalArgumentException("Propietario y precio válido son obligatorios");
        }
        Entrada entry = entradaRepository.findByIdForUpdate(idEntrada)
                .orElseThrow(() -> new IllegalArgumentException("Entrada no encontrada"));
        if (!EstadoEntrada.VENDIDO.equals(entry.getEstado()) || !request.idUsuario().equals(entry.getIdUsuario())) {
            throw new IllegalStateException("Solo el propietario puede publicar una entrada vendida");
        }
        BigDecimal originalPrice = entry.getPrecioOriginal() == null ? entry.getPrecio() : entry.getPrecioOriginal();
        BigDecimal maximumResalePrice = originalPrice == null ? null
                : originalPrice.multiply(new BigDecimal("0.95")).setScale(2, java.math.RoundingMode.DOWN);
        if (maximumResalePrice == null || request.precio().compareTo(maximumResalePrice) > 0) {
            throw new IllegalArgumentException("El precio de reventa no puede superar el 95 % del precio original");
        }
        asegurarEventoComprable(entry.getIdEvento());
        entry.setPrecioOriginal(originalPrice);
        entry.setEstado(EstadoEntrada.EN_REVENTA);
        entry.setIdOrden(null);
        entry.setIdReserva(null);
        entry.setIdUsuarioReserva(null);
        entry.setCodigoAcceso(null);
        entry.setPrecio(request.precio());
        return entradaRepository.save(entry);
    }

    @Override
    @Transactional
    public Entrada reservarReventa(Long idEntrada, TransferirEntradaRequest request) {
        if (request.idUsuarioDestino() == null || request.idOrden() == null) {
            throw new IllegalArgumentException("Comprador y orden son obligatorios");
        }
        Entrada entry = entradaRepository.findByIdForUpdate(idEntrada)
                .orElseThrow(() -> new IllegalArgumentException("Entrada no encontrada"));
        if (entry.getIdOrden() != null && entry.getIdOrden().equals(request.idOrden())
                && EstadoEntrada.EN_REVENTA.equals(entry.getEstado())
                && request.idUsuarioDestino().equals(entry.getIdUsuarioReserva())) {
            return entry;
        }
        if (!EstadoEntrada.EN_REVENTA.equals(entry.getEstado()) || entry.getIdOrden() != null
                || request.idUsuarioDestino().equals(entry.getIdUsuario())) {
            throw new IllegalStateException("La entrada ya no está disponible para reventa");
        }
        asegurarEventoComprable(entry.getIdEvento());
        entry.setIdOrden(request.idOrden());
        entry.setIdUsuarioReserva(request.idUsuarioDestino());
        entry.setFechaBloqueo(LocalDateTime.now());
        entry.setExpiraEn(LocalDateTime.now().plusMinutes(reservationMinutes));
        return entradaRepository.save(entry);
    }

    @Override
    @Transactional
    public void cancelarReservaReventa(Long idEntrada, TransferirEntradaRequest request) {
        if (request.idUsuarioDestino() == null || request.idOrden() == null) {
            throw new IllegalArgumentException("Comprador y orden son obligatorios para cancelar la reserva");
        }
        Entrada entry = entradaRepository.findByIdForUpdate(idEntrada)
                .orElseThrow(() -> new IllegalArgumentException("Entrada no encontrada"));
        if (EstadoEntrada.EN_REVENTA.equals(entry.getEstado())
                && entry.getIdOrden() == null && entry.getIdUsuarioReserva() == null) return;
        if (!EstadoEntrada.EN_REVENTA.equals(entry.getEstado())
                || !request.idOrden().equals(entry.getIdOrden())
                || !request.idUsuarioDestino().equals(entry.getIdUsuarioReserva())) {
            throw new IllegalStateException("La reserva de reventa no pertenece a esta orden");
        }
        entry.setIdOrden(null);
        entry.setIdUsuarioReserva(null);
        entry.setFechaBloqueo(null);
        entry.setExpiraEn(null);
        entradaRepository.save(entry);
    }

}