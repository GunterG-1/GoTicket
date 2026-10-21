package com.GoTicket.Inventario.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.GoTicket.Inventario.Model.Inventario;
import jakarta.persistence.LockModeType;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    Optional<Inventario> findByIdEvento(Long idEvento);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventario i where i.idEvento = :idEvento")
    Optional<Inventario> findByIdEventoForUpdate(@Param("idEvento") Long idEvento);
}
