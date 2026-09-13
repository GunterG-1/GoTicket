package com.GoTicket.Pagos.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.GoTicket.Pagos.Model.Pago;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByIdUsuario(Long idUsuario);

    List<Pago> findByIdOrden(Long idOrden);

    boolean existsByIdOrden(Long idOrden);
}
