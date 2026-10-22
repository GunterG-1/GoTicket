package com.GoTicket.Catalogo.Evento.Repository;

import com.GoTicket.Catalogo.Evento.Model.SectorEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectorEventoRepository extends JpaRepository<SectorEvento, Long> {
    List<SectorEvento> findByIdEventoOrderByIdSector(Long idEvento);
    void deleteByIdEvento(Long idEvento);
}
