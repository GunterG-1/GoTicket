package com.GoTicket.Inventario.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EstadoEntrada;

import jakarta.persistence.LockModeType;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {
    List<Entrada> findByIdEvento(Long idEvento);
    List<Entrada> findByIdEventoAndSectorAndEstado(Long idEvento, String sector, EstadoEntrada estado);
    List<Entrada> findByIdEventoAndEstado(Long idEvento, EstadoEntrada estado);
    List<Entrada> findByIdUsuarioAndEstado(Long idUsuario, EstadoEntrada estado);
    List<Entrada> findByIdUsuario(Long idUsuario);
    List<Entrada> findByEstado(EstadoEntrada estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Entrada e where e.idEntrada = :id")
    Optional<Entrada> findByIdForUpdate(@Param("id") Long id);

    List<Entrada> findByIdOrden(Long idOrden);
    List<Entrada> findByIdEventoAndIdUsuarioAndEstado(Long idEvento, Long idUsuario, com.GoTicket.Inventario.Model.EstadoEntrada estado);
    List<Entrada> findByEstadoAndExpiraEnBefore(com.GoTicket.Inventario.Model.EstadoEntrada estado, LocalDateTime time);
}