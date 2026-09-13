package com.GoTicket.Orden.Service;

import java.util.List;
import java.util.Optional;

import com.GoTicket.Orden.Model.Orden;

public interface OrdenService {

    List<Orden> listarOrdenes();

    Optional<Orden> buscarOrdenPorId(Long id);

    List<Orden> buscarOrdenesPorUsuario(Long idUsuario);

    Orden guardarOrden(Orden orden);

    Orden actualizarOrden(Long id, Orden orden);

    void eliminarOrden(Long id);
}
