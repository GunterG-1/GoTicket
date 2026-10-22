package com.GoTicket.Catalogo.Evento.Repository;

import com.GoTicket.Catalogo.Evento.Model.SectorRecinto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectorRecintoRepository extends JpaRepository<SectorRecinto, Long> {
    List<SectorRecinto> findByIdRecintoOrderByIdSectorRecinto(Long idRecinto);
    void deleteByIdRecinto(Long idRecinto);
}
