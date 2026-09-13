package com.GoTicket.Inventario.Service;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.GoTicket.Inventario.Model.Inventario;
import com.GoTicket.Inventario.Repository.InventarioRepository;
import com.GoTicket.Inventario.Repository.EntradaRepository;
import com.GoTicket.Inventario.Model.ConfirmarReservaRequest;
import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EntradaReservaRequest;
import com.GoTicket.Inventario.Model.EstadoEntrada;
import com.GoTicket.Inventario.Model.PublicarReventaRequest;
import com.GoTicket.Inventario.Model.TransferirEntradaRequest;
import com.GoTicket.Inventario.Model.TransferenciaEntrada;
import com.GoTicket.Inventario.Repository.TransferenciaEntradaRepository;
import com.GoTicket.Inventario.Client.CatalogoEvento;
import com.GoTicket.Inventario.Client.CatalogoEventoClient;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import com.GoTicket.Inventario.Repository.BloqueoInventarioRepository;
import com.GoTicket.Inventario.Messaging.PaymentStatusEvent;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventarioServiceImpl implements InventarioService {

    private final InventarioRepository inventarioRepository;
    private final EntradaRepository entradaRepository;
    private final StringRedisTemplate redisTemplate;
    private final TransferenciaEntradaRepository transferenciaRepository;
    private final CatalogoEventoClient catalogoEventoClient;
    private final BloqueoInventarioRepository bloqueoRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${goticket.reservation.minutes:5}")
    private long reservationMinutes = 5;
    @Value("${goticket.rabbit.exchange}")
    private String exchangeName;
    @Value("${goticket.rabbit.payment-failed-key}")
    private String paymentFailedKey;

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
    @Transactional
    public Inventario guardarInventario(Inventario inventario) {
        if (inventario.getCantidadDisponible() == null) {
            inventario.setCantidadDisponible(0);
        }
        if (inventario.getCantidadTotal() == null) {
            inventario.setCantidadTotal(0);
        }
        inventario.setFechaActualizacion(LocalDateTime.now());
        Inventario saved = inventarioRepository.save(inventario);
        return saved;
    }

    @Override
    @Transactional
    public Inventario actualizarInventario(Long id, Inventario inventario) {
        Inventario inventarioExistente = inventarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventario no encontrado con id: " + id));

        inventarioExistente.setIdEvento(inventario.getIdEvento());
        inventarioExistente.setCantidadDisponible(inventario.getCantidadDisponible());
        inventarioExistente.setCantidadTotal(inventario.getCantidadTotal());
        inventarioExistente.setActivo(inventario.getActivo());
        inventarioExistente.setFechaActualizacion(LocalDateTime.now());

        return inventarioRepository.save(inventarioExistente);
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
    @Transactional(readOnly = true)
    public List<Entrada> listarEntradas(Long idEvento) {
        return entradaRepository.findByIdEvento(idEvento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entrada> listarEntradasDeUsuario(Long idUsuario) {
        return entradaRepository.findByIdUsuario(idUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entrada> listarEntradasEnReventa() {
        return entradaRepository.findByEstado(EstadoEntrada.EN_REVENTA);
    }

    @Override
    @Transactional
    public List<Entrada> reservarEntradas(EntradaReservaRequest request) {
        if (request.idEvento() == null || request.idUsuario() == null || request.cantidad() == null || request.cantidad() <= 0) {
            throw new IllegalArgumentException("Evento, usuario y cantidad válida son obligatorios");
        }
        String sector = request.sector() == null || request.sector().isBlank() ? "General" : request.sector();
        asegurarEntradasFisicas(request.idEvento(), request.cantidad());
        List<Entrada> available = entradaRepository.findByIdEventoAndSectorAndEstado(request.idEvento(), sector, EstadoEntrada.DISPONIBLE);
        if (available.size() < request.cantidad()) {
            available = entradaRepository.findByIdEventoAndEstado(request.idEvento(), EstadoEntrada.DISPONIBLE);
        }
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

    private void asegurarEntradasFisicas(Long idEvento, int requested) {
        CatalogoEvento event = catalogoEventoClient.buscarEvento(idEvento)
                .orElseThrow(() -> new IllegalStateException("No se pudo obtener el evento desde Catalogo-Evento"));
        Inventario inventory = inventarioRepository.findByIdEvento(idEvento).orElseGet(() -> {
            int capacity = event.capacidad() == null ? requested : event.capacidad();
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

        List<Entrada> existingAvailable = entradaRepository.findByIdEventoAndEstado(idEvento, EstadoEntrada.DISPONIBLE);
        int missing = requested - existingAvailable.size();
        if (missing <= 0) return;

        int existingCount = entradaRepository.findByIdEvento(idEvento).size();
        int capacity = inventory.getCantidadTotal() == null ? existingCount + missing : inventory.getCantidadTotal();
        int toCreate = Math.min(missing, Math.max(0, capacity - existingCount));
        if (toCreate < missing) {
            throw new IllegalStateException("No hay entradas físicas suficientes para el evento solicitado");
        }

        List<Entrada> newEntries = new ArrayList<>();
        for (int index = 1; index <= toCreate; index++) {
            newEntries.add(new Entrada(null, idEvento, "General", String.valueOf(existingCount + index),
                        event.precio() == null ? BigDecimal.ZERO : event.precio(), EstadoEntrada.DISPONIBLE,
                        null, null, null, null));
        }
        entradaRepository.saveAll(newEntries);
    }

    @Override
    @Transactional
    public void confirmarReserva(ConfirmarReservaRequest request) {
        if (request.idEvento() == null || request.idUsuario() == null || request.idOrden() == null) {
            throw new IllegalArgumentException("Evento, usuario y orden son obligatorios");
        }
        List<Entrada> entries = entradaRepository.findByIdEventoAndIdUsuarioAndEstado(request.idEvento(), request.idUsuario(), EstadoEntrada.BLOQUEADO);
        if (entries.isEmpty()) {
            throw new IllegalStateException("La reserva no tiene entradas asociadas");
        }
        entries.forEach(entry -> entry.setIdOrden(request.idOrden()));
        entradaRepository.saveAll(entries);
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void liberarEntradasExpiradas() {
        Map<Long, Integer> expiredOrders = new LinkedHashMap<>();
        Map<Long, Entrada> orderEntries = new LinkedHashMap<>();
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
            entry.setFechaBloqueo(null);
            entry.setExpiraEn(null);
            redisTemplate.delete("goticket:entrada:" + entry.getIdEntrada());
            entradaRepository.save(entry);
        });
        expiredOrders.forEach((orderId, quantity) -> {
            Entrada entry = orderEntries.get(orderId);
            rabbitTemplate.convertAndSend(exchangeName, paymentFailedKey,
                    new PaymentStatusEvent(null, orderId, entry.getIdEvento(), entry.getIdUsuario(), quantity, null, "RECHAZADO"));
        });
    }

    @Override
    @Transactional
    public Entrada publicarReventa(Long idEntrada, PublicarReventaRequest request) {
        Entrada entry = entradaRepository.findByIdForUpdate(idEntrada)
                .orElseThrow(() -> new IllegalArgumentException("Entrada no encontrada"));
        if (!EstadoEntrada.VENDIDO.equals(entry.getEstado()) || !request.idUsuario().equals(entry.getIdUsuario())) {
            throw new IllegalStateException("Solo el propietario puede publicar una entrada vendida");
        }
        entry.setEstado(EstadoEntrada.EN_REVENTA);
        entry.setPrecio(request.precio());
        return entradaRepository.save(entry);
    }

    @Override
    @Transactional
    public Entrada transferirEntrada(Long idEntrada, TransferirEntradaRequest request) {
        if (request.idUsuarioDestino() == null || request.idOrden() == null) {
            throw new IllegalArgumentException("Usuario destino y orden son obligatorios");
        }
        Entrada entry = entradaRepository.findByIdForUpdate(idEntrada)
                .orElseThrow(() -> new IllegalArgumentException("Entrada no encontrada"));
        if (!EstadoEntrada.EN_REVENTA.equals(entry.getEstado())) {
            throw new IllegalStateException("La entrada no está publicada para reventa");
        }
        Long previousOwner = entry.getIdUsuario();
        entry.setIdUsuario(request.idUsuarioDestino());
        entry.setIdOrden(request.idOrden());
        entry.setEstado(EstadoEntrada.VENDIDO);
        entradaRepository.save(entry);
        transferenciaRepository.save(new TransferenciaEntrada(null, idEntrada, previousOwner,
                request.idUsuarioDestino(), request.idOrden(), LocalDateTime.now()));
        return entry;
    }
}
