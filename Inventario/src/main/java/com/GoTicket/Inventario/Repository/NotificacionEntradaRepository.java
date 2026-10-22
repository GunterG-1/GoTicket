package com.GoTicket.Inventario.Repository;

import com.GoTicket.Inventario.Model.NotificacionEntrada;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificacionEntradaRepository extends JpaRepository<NotificacionEntrada, Long> {
    Optional<NotificacionEntrada> findByIdOrden(Long idOrden);

    List<NotificacionEntrada> findTop20ByEstadoAndProximoIntentoLessThanEqualOrderByIdNotificacionAsc(
            String estado, LocalDateTime fecha);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select notification from NotificacionEntrada notification where notification.idNotificacion = :id")
    Optional<NotificacionEntrada> findByIdForUpdate(@Param("id") Long id);
}
