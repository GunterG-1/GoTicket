package com.GoTicket.Catalogo.Evento.Service;

import com.GoTicket.Catalogo.Evento.Model.Recinto;
import com.GoTicket.Catalogo.Evento.Repository.EventoRepository;
import com.GoTicket.Catalogo.Evento.Repository.RecintoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecintoService {
    private final RecintoRepository recintoRepository;
    private final EventoRepository eventoRepository;

    @Transactional(readOnly = true)
    public List<Recinto> listar() {
        return recintoRepository.findAllByOrderByNombreAsc();
    }

    @Transactional
    public Recinto guardar(Recinto recinto) {
        validar(recinto, null);
        recinto.setIdRecinto(null);
        return recintoRepository.save(recinto);
    }

    @Transactional
    public Recinto actualizar(Long id, Recinto cambios) {
        Recinto actual = recintoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recinto no encontrado"));
        validar(cambios, id);
        if (eventoRepository.existsByIdRecinto(id) && cambios.getCapacidad() < capacidadComprometida(id)) {
            throw new IllegalArgumentException("La capacidad del recinto no puede quedar bajo la capacidad de sus eventos");
        }
        actual.setNombre(cambios.getNombre().trim());
        actual.setCiudad(cambios.getCiudad().trim());
        actual.setDireccion(cambios.getDireccion().trim());
        actual.setCapacidad(cambios.getCapacidad());
        return recintoRepository.save(actual);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!recintoRepository.existsById(id)) {
            throw new IllegalArgumentException("Recinto no encontrado");
        }
        if (eventoRepository.existsByIdRecinto(id)) {
            throw new IllegalStateException("No se puede eliminar un recinto asignado a eventos");
        }
        recintoRepository.deleteById(id);
    }

    private int capacidadComprometida(Long idRecinto) {
        return eventoRepository.findByIdRecinto(idRecinto).stream()
                .mapToInt(evento -> evento.getCapacidad() == null ? 0 : evento.getCapacidad())
                .max().orElse(0);
    }

    private void validar(Recinto recinto, Long idRecinto) {
        if (recinto == null || recinto.getNombre() == null || recinto.getNombre().isBlank()
                || recinto.getCiudad() == null || recinto.getCiudad().isBlank()
                || recinto.getDireccion() == null || recinto.getDireccion().isBlank()
                || recinto.getCapacidad() == null || recinto.getCapacidad() <= 0) {
            throw new IllegalArgumentException("Nombre, ciudad, dirección y capacidad positiva son obligatorios");
        }
        if (idRecinto == null) {
            if (recintoRepository.existsByNombreIgnoreCaseAndCiudadIgnoreCase(
                    recinto.getNombre().trim(), recinto.getCiudad().trim())) {
                throw new IllegalArgumentException("Ya existe un recinto con ese nombre en la ciudad");
            }
        } else if (recintoRepository.existsByNombreIgnoreCaseAndCiudadIgnoreCaseAndIdRecintoNot(
                recinto.getNombre().trim(), recinto.getCiudad().trim(), idRecinto)) {
            throw new IllegalArgumentException("Ya existe un recinto con ese nombre en la ciudad");
        }
    }
}
