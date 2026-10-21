package com.GoTicket.Inventario.Service;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Collectors;

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
import com.GoTicket.Inventario.Model.SectorDisponibilidad;
import com.GoTicket.Inventario.Repository.TransferenciaEntradaRepository;
import com.GoTicket.Inventario.Client.CatalogoEvento;
import com.GoTicket.Inventario.Client.CatalogoEventoClient;

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
        CatalogoEvento event = catalogoEventoClient.buscarEvento(idEvento)
                .orElseThrow(() -> new IllegalStateException("No se pudo obtener el evento desde Catalogo-Evento"));
        Map<String, Integer> capacities = calcularCapacidadSectores(event, inventory.getCantidadTotal());
        Map<String, Long> committed = entradaRepository.findByIdEvento(idEvento).stream()
                .filter(entry -> entry.getEstado() != EstadoEntrada.DISPONIBLE)
                .collect(Collectors.groupingBy(Entrada::getSector, Collectors.counting()));
        return capacities.entrySet().stream()
                .map(entry -> new SectorDisponibilidad(entry.getKey(), entry.getValue(),
                        Math.max(0, entry.getValue() - committed.getOrDefault(entry.getKey(), 0L).intValue())))
                .toList();
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

        int nuevaCantidadTotal = inventario.getCantidadTotal() == null ? 0 : inventario.getCantidadTotal();
        if (nuevaCantidadTotal <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor que cero");
        }
        int cantidadTotalAnterior = inventarioExistente.getCantidadTotal() == null
                ? 0 : inventarioExistente.getCantidadTotal();
        int cantidadDisponibleAnterior = inventarioExistente.getCantidadDisponible() == null
                ? 0 : inventarioExistente.getCantidadDisponible();
        long entradasComprometidas = entradaRepository.findByIdEvento(inventarioExistente.getIdEvento()).stream()
                .filter(entrada -> entrada.getEstado() != EstadoEntrada.DISPONIBLE)
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
    public List<Entrada> listarEntradasDeUsuario(Long idUsuario) {
        return entradaRepository.findByIdUsuario(idUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entrada> listarEntradasEnReventa() {
        return entradaRepository.findByEstadoAndIdOrdenIsNull(EstadoEntrada.EN_REVENTA);
    }

    @Override
    @Transactional
    public List<Entrada> reservarEntradas(EntradaReservaRequest request) {
        if (request.idEvento() == null || request.idUsuario() == null || request.cantidad() == null || request.cantidad() <= 0) {
            throw new IllegalArgumentException("Evento, usuario y cantidad vÃ¡lida son obligatorios");
        }
        String sector = request.sector() == null || request.sector().isBlank() ? "General" : request.sector().trim();
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

    private void asegurarEntradasFisicas(Long idEvento, String sector, int requested) {
        CatalogoEvento event = catalogoEventoClient.buscarEvento(idEvento)
                .orElseThrow(() -> new IllegalStateException("No se pudo obtener el evento desde Catalogo-Evento"));
        Inventario inventory = inventarioRepository.findByIdEventoForUpdate(idEvento).orElseGet(() -> {
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

        Map<String, Integer> sectorCapacities = calcularCapacidadSectores(event, inventory.getCantidadTotal());
        Integer sectorCapacity = sectorCapacities.get(sector);
        if (sectorCapacity == null) {
            throw new IllegalArgumentException("El sector solicitado no corresponde a este evento");
        }
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
        for (int index = 1; index <= toCreate; index++) {
            newEntries.add(new Entrada(null, idEvento, sector, String.valueOf(existingCount + index),
                        event.precio() == null ? BigDecimal.ZERO : event.precio(), EstadoEntrada.DISPONIBLE,
                        null, null, null, null));
        }
        entradaRepository.saveAll(newEntries);
    }

    private Map<String, Integer> calcularCapacidadSectores(CatalogoEvento event, int totalCapacity) {
        String nameAndCategory = ((event.nombre() == null ? "" : event.nombre()) + " "
                + (event.categoria() == null ? "" : event.categoria())).toLowerCase(Locale.ROOT);
        Map<String, Integer> weights = new LinkedHashMap<>();
        if (nameAndCategory.matches(".*(f[uú]tbol|futbol|partido|colo-colo|deportes concepci[oó]n).*")) {
            weights.put("Rapa Nui", 1800);
            weights.put("Oceano", 2400);
            weights.put("Cordillera", 3000);
            weights.put("Caupolicán", 2600);
            weights.put("Tucapel", 2600);
            weights.put("Galvarino", 2200);
            weights.put("Arica", 2200);
            weights.put("Magallanes", 2200);
        } else if (event.lugar() != null && event.lugar().toLowerCase(Locale.ROOT).contains("santander arena")) {
            weights.put("Tribuna", 2500);
            weights.put("Platea Alta", 2200);
            weights.put("Platea Baja", 1800);
            weights.put("Cancha General", 3200);
            weights.put("Platea Baja VIP", 900);
        } else {
            weights.put("General", 1);
        }

        int weightTotal = weights.values().stream().mapToInt(Integer::intValue).sum();
        Map<String, Integer> capacities = new LinkedHashMap<>();
        Map<String, Double> remainders = new LinkedHashMap<>();
        int allocated = 0;
        for (Map.Entry<String, Integer> sector : weights.entrySet()) {
            double exact = (double) totalCapacity * sector.getValue() / weightTotal;
            int base = (int) Math.floor(exact);
            capacities.put(sector.getKey(), base);
            remainders.put(sector.getKey(), exact - base);
            allocated += base;
        }
        remainders.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(totalCapacity - allocated)
                .forEach(entry -> capacities.compute(entry.getKey(), (key, value) -> value + 1));
        return capacities;
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
        Inventario inventory = inventarioRepository.findByIdEventoForUpdate(request.idEvento())
                .orElseThrow(() -> new IllegalStateException("Inventario no encontrado para el evento"));
        if (bloqueoRepository.findByIdOrden(request.idOrden()).isEmpty()) {
            int available = inventory.getCantidadDisponible() == null ? 0 : inventory.getCantidadDisponible();
            if (available < entries.size()) {
                throw new IllegalStateException("No hay disponibilidad suficiente para confirmar la reserva");
            }
            inventory.setCantidadDisponible(available - entries.size());
            inventory.setFechaActualizacion(LocalDateTime.now());
            inventarioRepository.save(inventory);
            bloqueoRepository.save(new com.GoTicket.Inventario.Model.BloqueoInventario(
                    null, request.idOrden(), request.idEvento(), entries.size(),
                    LocalDateTime.now().plusMinutes(reservationMinutes), false));
        }
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
        entry.setEstado(EstadoEntrada.EN_REVENTA);
        entry.setIdOrden(null);
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
                && EstadoEntrada.EN_REVENTA.equals(entry.getEstado())) {
            return entry;
        }
        if (!EstadoEntrada.EN_REVENTA.equals(entry.getEstado()) || entry.getIdOrden() != null
                || request.idUsuarioDestino().equals(entry.getIdUsuario())) {
            throw new IllegalStateException("La entrada ya no está disponible para reventa");
        }
        entry.setIdOrden(request.idOrden());
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
            throw new IllegalStateException("La entrada no estÃ¡ publicada para reventa");
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