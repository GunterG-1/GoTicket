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
    Optional<Entrada> findByCodigoAcceso(String codigoAcceso);
    List<Entrada> findByEstado(EstadoEntrada estado);
    List<Entrada> findByEstadoAndIdOrdenIsNull(EstadoEntrada estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Entrada e where e.idEntrada = :id")
    Optional<Entrada> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Entrada e where e.codigoAcceso = :codigoAcceso")
    Optional<Entrada> findByCodigoAccesoForUpdate(@Param("codigoAcceso") String codigoAcceso);

    List<Entrada> findByIdOrden(Long idOrden);
    List<Entrada> findByIdEventoAndIdUsuarioAndEstado(Long idEvento, Long idUsuario, com.GoTicket.Inventario.Model.EstadoEntrada estado);
    List<Entrada> findByIdEventoAndIdUsuarioAndIdReservaAndEstado(
            Long idEvento, Long idUsuario, String idReserva, EstadoEntrada estado);
    List<Entrada> findByIdReservaAndEstado(String idReserva, EstadoEntrada estado);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Entrada e where e.idReserva = :idReserva and e.estado = :estado")
    List<Entrada> findByIdReservaAndEstadoForUpdate(
            @Param("idReserva") String idReserva, @Param("estado") EstadoEntrada estado);
    List<Entrada> findByEstadoAndExpiraEnBefore(com.GoTicket.Inventario.Model.EstadoEntrada estado, LocalDateTime time);
}