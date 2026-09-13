package com.GoTicket.Inventario.Repository;

import com.GoTicket.Inventario.Model.BloqueoInventario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloqueoInventarioRepository extends JpaRepository<BloqueoInventario, Long> {
    Optional<BloqueoInventario> findByIdOrden(Long idOrden);
    List<BloqueoInventario> findByResueltoFalseAndExpiraEnBefore(LocalDateTime time);
}