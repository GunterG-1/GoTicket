package com.GoTicket.Inventario.Repository;

import com.GoTicket.Inventario.Model.BloqueoInventario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloqueoInventarioRepository extends JpaRepository<BloqueoInventario, Long> {
    Optional<BloqueoInventario> findByIdOrden(Long idOrden);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BloqueoInventario b where b.idOrden = :idOrden")
    Optional<BloqueoInventario> findByIdOrdenForUpdate(@Param("idOrden") Long idOrden);
    List<BloqueoInventario> findByResueltoFalseAndExpiraEnBefore(LocalDateTime time);
}