package com.GoTicket.Inventario.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.GoTicket.Inventario.Model.Inventario;
import jakarta.persistence.LockModeType;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    Optional<Inventario> findByIdEvento(Long idEvento);

    @Modifying
    @Query(value = """
            INSERT INTO inventario (id_evento, cantidad_disponible, cantidad_total, activo, fecha_actualizacion)
            VALUES (:idEvento, :cantidadDisponible, :cantidadTotal, :activo, :fechaActualizacion)
            ON DUPLICATE KEY UPDATE id_evento = VALUES(id_evento)
            """, nativeQuery = true)
    int crearSiNoExiste(
            @Param("idEvento") Long idEvento,
            @Param("cantidadDisponible") Integer cantidadDisponible,
            @Param("cantidadTotal") Integer cantidadTotal,
            @Param("activo") Boolean activo,
            @Param("fechaActualizacion") LocalDateTime fechaActualizacion);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventario i where i.idEvento = :idEvento")
    Optional<Inventario> findByIdEventoForUpdate(@Param("idEvento") Long idEvento);
}
