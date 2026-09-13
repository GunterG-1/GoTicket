package com.GoTicket.Inventario.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.GoTicket.Inventario.Model.TransferenciaEntrada;

public interface TransferenciaEntradaRepository extends JpaRepository<TransferenciaEntrada, Long> {
    List<TransferenciaEntrada> findByIdEntradaOrderByFechaDesc(Long idEntrada);
}