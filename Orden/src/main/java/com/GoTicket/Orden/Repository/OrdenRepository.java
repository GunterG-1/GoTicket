package com.GoTicket.Orden.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.GoTicket.Orden.Model.Orden;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, Long> {

    List<Orden> findByIdUsuario(Long idUsuario);

    List<Orden> findByIdEvento(Long idEvento);
}
