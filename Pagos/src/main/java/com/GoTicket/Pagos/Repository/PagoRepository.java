package com.GoTicket.Pagos.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import com.GoTicket.Pagos.Model.Pago;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByIdUsuario(Long idUsuario);

    List<Pago> findByIdOrden(Long idOrden);

    boolean existsByIdOrden(Long idOrden);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.idPago = :id")
    Optional<Pago> findByIdForUpdate(@Param("id") Long id);
}
