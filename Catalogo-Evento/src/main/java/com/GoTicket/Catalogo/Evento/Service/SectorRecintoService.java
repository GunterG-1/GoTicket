package com.GoTicket.Catalogo.Evento.Service;

import com.GoTicket.Catalogo.Evento.Model.Recinto;
import com.GoTicket.Catalogo.Evento.Model.SectorRecinto;
import com.GoTicket.Catalogo.Evento.Model.SectorRecintoRequest;
import com.GoTicket.Catalogo.Evento.Repository.RecintoRepository;
import com.GoTicket.Catalogo.Evento.Repository.SectorRecintoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SectorRecintoService {
    private static final String DEFAULT_SECTOR_COLOR = "#702bd8";
    private final RecintoRepository recintoRepository;
    private final SectorRecintoRepository sectorRepository;

    @Transactional(readOnly = true)
    public List<SectorRecinto> listar(Long idRecinto) {
        if (!recintoRepository.existsById(idRecinto)) {
            throw new IllegalArgumentException("Recinto no encontrado");
        }
        return sectorRepository.findByIdRecintoOrderByIdSectorRecinto(idRecinto);
    }

    @Transactional
    public List<SectorRecinto> configurar(Long idRecinto, List<SectorRecintoRequest> requests) {
        Recinto recinto = recintoRepository.findById(idRecinto)
                .orElseThrow(() -> new IllegalArgumentException("Recinto no encontrado"));
        validar(recinto, requests);
        sectorRepository.deleteByIdRecinto(idRecinto);
        return sectorRepository.saveAll(requests.stream().map(request -> new SectorRecinto(
                null, idRecinto, request.nombre().trim(), request.capacidad(),
                request.modalidad().toUpperCase(Locale.ROOT),
                request.color() == null || request.color().isBlank() ? null : request.color().trim(),
                request.posicionX(), request.posicionY())).toList());
    }

    @Transactional
    public void crearMapaInicialSiFalta(Long idRecinto) {
        List<SectorRecinto> existing = sectorRepository.findByIdRecintoOrderByIdSectorRecinto(idRecinto);
        if (!existing.isEmpty() && !(existing.size() == 1
                && "General".equalsIgnoreCase(existing.get(0).getNombre())
                && DEFAULT_SECTOR_COLOR.equalsIgnoreCase(existing.get(0).getColor())
                && Integer.valueOf(50).equals(existing.get(0).getPosicionX())
                && Integer.valueOf(35).equals(existing.get(0).getPosicionY()))) {
            return;
        }
        Recinto recinto = recintoRepository.findById(idRecinto)
                .orElseThrow(() -> new IllegalArgumentException("Recinto no encontrado"));
        if (existing.size() == 1 && !existing.get(0).getCapacidad().equals(recinto.getCapacidad())) {
            return;
        }
        if (!existing.isEmpty()) {
            sectorRepository.deleteByIdRecinto(idRecinto);
            sectorRepository.flush();
        }
        sectorRepository.saveAll(crearSectoresPlantilla(idRecinto, recinto));
    }

    private List<SectorRecinto> crearSectoresPlantilla(Long idRecinto, Recinto recinto) {
        String venueName = recinto.getNombre().toLowerCase(Locale.ROOT);
        List<SectorPlantilla> template;
        if (venueName.contains("estadio")) {
            template = List.of(
                    new SectorPlantilla("Cancha", 25, "GENERAL", "#f97316", 50, 72),
                    new SectorPlantilla("Tribuna Andes", 25, "NUMERADO", "#2563eb", 28, 46),
                    new SectorPlantilla("Tribuna Pacífico", 25, "NUMERADO", "#7c3aed", 72, 46),
                    new SectorPlantilla("Galería Norte", 12, "GENERAL", "#16a34a", 30, 22),
                    new SectorPlantilla("Galería Sur", 13, "GENERAL", "#db2777", 70, 22));
        } else if (venueName.contains("teatro")) {
            template = List.of(
                    new SectorPlantilla("Platea", 40, "NUMERADO", "#f97316", 50, 70),
                    new SectorPlantilla("Palcos", 20, "NUMERADO", "#d97706", 50, 48),
                    new SectorPlantilla("Balcón", 20, "NUMERADO", "#2563eb", 50, 28),
                    new SectorPlantilla("Galería", 20, "NUMERADO", "#16a34a", 50, 12));
        } else if (venueName.contains("arena") || venueName.contains("riesco")) {
            template = List.of(
                    new SectorPlantilla("Cancha", 35, "GENERAL", "#f97316", 50, 72),
                    new SectorPlantilla("Platea Baja", 30, "NUMERADO", "#2563eb", 27, 48),
                    new SectorPlantilla("Platea Alta", 20, "NUMERADO", "#7c3aed", 73, 40),
                    new SectorPlantilla("VIP", 15, "NUMERADO", "#d97706", 50, 20));
        } else {
            template = List.of(
                    new SectorPlantilla("General", 60, "GENERAL", "#16a34a", 50, 68),
                    new SectorPlantilla("Preferencial", 25, "GENERAL", "#2563eb", 50, 42),
                    new SectorPlantilla("VIP", 15, "NUMERADO", "#d97706", 50, 18));
        }

        List<Integer> capacities = distribuirCapacidad(recinto.getCapacidad(),
                template.stream().map(SectorPlantilla::peso).toList());
        return java.util.stream.IntStream.range(0, template.size())
                .mapToObj(index -> {
                    SectorPlantilla sector = template.get(index);
                    return new SectorRecinto(null, idRecinto, sector.nombre(), capacities.get(index),
                            sector.modalidad(), sector.color(), sector.posicionX(), sector.posicionY());
                })
                .toList();
    }

    static List<Integer> distribuirCapacidad(int capacidad, List<Integer> pesos) {
        int pesoTotal = pesos.stream().mapToInt(Integer::intValue).sum();
        List<Integer> capacidades = new java.util.ArrayList<>(pesos.size());
        int asignada = 0;
        for (int index = 0; index < pesos.size(); index++) {
            int cantidad = index == pesos.size() - 1
                    ? capacidad - asignada
                    : (int) ((long) capacidad * pesos.get(index) / pesoTotal);
            capacidades.add(cantidad);
            asignada += cantidad;
        }
        return capacidades;
    }

    private record SectorPlantilla(String nombre, int peso, String modalidad, String color,
            int posicionX, int posicionY) {
    }

    private void validar(Recinto recinto, List<SectorRecintoRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Configura al menos un sector para el mapa del recinto");
        }
        HashSet<String> names = new HashSet<>();
        int totalCapacity = 0;
        for (SectorRecintoRequest request : requests) {
            if (request == null || request.nombre() == null || request.nombre().isBlank()
                    || request.capacidad() == null || request.capacidad() <= 0
                    || request.posicionX() == null || request.posicionX() < 0 || request.posicionX() > 100
                    || request.posicionY() == null || request.posicionY() < 0 || request.posicionY() > 100) {
                throw new IllegalArgumentException(
                        "Cada sector requiere nombre, capacidad positiva y posición entre 0 y 100");
            }
            if (!names.add(request.nombre().trim().toLowerCase(Locale.ROOT))) {
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
            if (request.capacidad() > recinto.getCapacidad() - totalCapacity) {
                throw new IllegalArgumentException("La capacidad de los sectores debe coincidir con el aforo del recinto");
            }
            totalCapacity += request.capacidad();
        }
        if (totalCapacity != recinto.getCapacidad()) {
            throw new IllegalArgumentException("La capacidad de los sectores debe coincidir con el aforo del recinto");
        }
    }
}
