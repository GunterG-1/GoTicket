package com.GoTicket.Catalogo.Evento.Service;

import com.GoTicket.Catalogo.Evento.Model.Evento;
import com.GoTicket.Catalogo.Evento.Model.SectorEvento;
import com.GoTicket.Catalogo.Evento.Model.SectorEventoRequest;
import com.GoTicket.Catalogo.Evento.Model.SectorRecinto;
import com.GoTicket.Catalogo.Evento.Repository.EventoRepository;
import com.GoTicket.Catalogo.Evento.Repository.SectorRecintoRepository;
import com.GoTicket.Catalogo.Evento.Repository.SectorEventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SectorEventoService {
    private final EventoRepository eventoRepository;
    private final SectorEventoRepository sectorRepository;
    private final SectorRecintoRepository sectorRecintoRepository;

    @Transactional(readOnly = true)
    public List<SectorEvento> listar(Long idEvento) {
        if (!eventoRepository.existsById(idEvento)) {
            throw new IllegalArgumentException("Evento no encontrado");
        }
        return sectorRepository.findByIdEventoOrderByIdSector(idEvento);
    }

    @Transactional
    public void crearSectorGeneralSiFaltan(Long idEvento) {
        if (!sectorRepository.findByIdEventoOrderByIdSector(idEvento).isEmpty()) {
            return;
        }
        Evento event = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));
        if (event.getCapacidad() == null || event.getCapacidad() <= 0
                || event.getPrecio() == null || event.getPrecio().signum() < 0) {
            throw new IllegalStateException("No se puede crear el sector General: capacidad o precio inválido");
        }
        sectorRepository.save(new SectorEvento(null, idEvento, "General", event.getCapacidad(),
                event.getPrecio(), "GENERAL", "#702bd8", 50, 35));
    }

    @Transactional
    public void configurarSectoresInicialesDesdeRecinto(Long idEvento, Long idRecinto) {
        Evento event = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));
        List<SectorEvento> existing = sectorRepository.findByIdEventoOrderByIdSector(idEvento);
        boolean defaultGeneral = existing.isEmpty() || (existing.size() == 1
                && "General".equalsIgnoreCase(existing.get(0).getNombre())
                && existing.get(0).getCapacidad().equals(event.getCapacidad())
                && existing.get(0).getPrecio().compareTo(event.getPrecio()) == 0
                && "#702bd8".equalsIgnoreCase(existing.get(0).getColor())
                && Integer.valueOf(50).equals(existing.get(0).getPosicionX())
                && Integer.valueOf(35).equals(existing.get(0).getPosicionY()));
        if (!defaultGeneral) {
            return;
        }
        if (event.getCapacidad() == null || event.getCapacidad() <= 0
                || event.getPrecio() == null || event.getPrecio().signum() < 0) {
            throw new IllegalStateException("No se pueden crear sectores: capacidad o precio del evento inválido");
        }
        List<SectorRecinto> venueSectors = sectorRecintoRepository
                .findByIdRecintoOrderByIdSectorRecinto(idRecinto);
        if (venueSectors.isEmpty()) {
            throw new IllegalStateException("El recinto no tiene un mapa de sectores configurado");
        }
        int venueCapacity = venueSectors.stream().mapToInt(SectorRecinto::getCapacidad).sum();
        List<Integer> capacities = SectorRecintoService.distribuirCapacidad(event.getCapacidad(),
                venueSectors.stream().map(SectorRecinto::getCapacidad).toList());
        if (existing.size() == 1) {
            sectorRepository.deleteByIdEvento(idEvento);
            sectorRepository.flush();
        }
        List<SectorEvento> eventSectors = java.util.stream.IntStream.range(0, venueSectors.size())
                .mapToObj(index -> {
                    SectorRecinto venueSector = venueSectors.get(index);
                    return new SectorEvento(null, idEvento, venueSector.getNombre(), capacities.get(index),
                            event.getPrecio(), venueSector.getModalidad(), venueSector.getColor(),
                            venueSector.getPosicionX(), venueSector.getPosicionY());
                })
                .toList();
        if (venueCapacity <= 0 || capacities.stream().mapToInt(Integer::intValue).sum() != event.getCapacidad()) {
            throw new IllegalStateException("No se pudo distribuir la capacidad del evento entre sus sectores");
        }
        sectorRepository.saveAll(eventSectors);
    }

    @Transactional
    public List<SectorEvento> configurar(Long idEvento, List<SectorEventoRequest> requests) {
        Evento event = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Configura al menos un sector");
        }
        HashSet<String> names = new HashSet<>();
        int totalCapacity = 0;
        for (SectorEventoRequest request : requests) {
            if (request == null || request.nombre() == null || request.nombre().isBlank()
                    || request.capacidad() == null || request.capacidad() <= 0
                    || request.precio() == null || request.precio().signum() < 0
                    || request.precio().scale() > 2 || request.precio().precision() > 10
                    || request.posicionX() == null || request.posicionX() < 0 || request.posicionX() > 100
                    || request.posicionY() == null || request.posicionY() < 0 || request.posicionY() > 100) {
                throw new IllegalArgumentException("Cada sector requiere nombre, capacidad positiva y precio no negativo");
            }
            String name = request.nombre().trim();
            if (!names.add(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Los nombres de sectores no pueden repetirse");
            }
            if (!"GENERAL".equalsIgnoreCase(request.modalidad())
                    && !"NUMERADO".equalsIgnoreCase(request.modalidad())) {
                throw new IllegalArgumentException("La modalidad debe ser GENERAL o NUMERADO");
            }
            if (request.color() != null && !request.color().isBlank()
                    && !request.color().trim().matches("^#[0-9a-fA-F]{6}$")) {
                throw new IllegalArgumentException("El color debe ser un valor hexadecimal de seis dígitos");
            }
            if (event.getCapacidad() == null || request.capacidad() > event.getCapacidad() - totalCapacity) {
                throw new IllegalArgumentException("La suma de capacidades de sectores debe coincidir con la capacidad del evento");
            }
            totalCapacity += request.capacidad();
        }
        if (event.getCapacidad() == null || totalCapacity != event.getCapacidad()) {
            throw new IllegalArgumentException("La suma de capacidades de sectores debe coincidir con la capacidad del evento");
        }

        sectorRepository.deleteByIdEvento(idEvento);
        sectorRepository.flush();
        List<SectorEvento> sectors = requests.stream().map(request -> new SectorEvento(null, idEvento,
                request.nombre().trim(), request.capacidad(), request.precio(),
                request.modalidad().toUpperCase(Locale.ROOT),
                request.color() == null || request.color().isBlank() ? null : request.color().trim(),
                request.posicionX(), request.posicionY())).toList();
        return sectorRepository.saveAll(sectors);
    }
}
