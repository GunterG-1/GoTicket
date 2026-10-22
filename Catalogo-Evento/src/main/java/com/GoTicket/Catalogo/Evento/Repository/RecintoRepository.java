package com.GoTicket.Catalogo.Evento.Repository;

import com.GoTicket.Catalogo.Evento.Model.Recinto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecintoRepository extends JpaRepository<Recinto, Long> {
    List<Recinto> findAllByOrderByNombreAsc();
    Optional<Recinto> findByNombreIgnoreCaseAndCiudadIgnoreCase(String nombre, String ciudad);
    boolean existsByNombreIgnoreCaseAndCiudadIgnoreCase(String nombre, String ciudad);
    boolean existsByNombreIgnoreCaseAndCiudadIgnoreCaseAndIdRecintoNot(
            String nombre, String ciudad, Long idRecinto);
}
