package com.GoTicket.Orden.Service;

import java.util.List;
import java.util.Optional;

import com.GoTicket.Orden.Model.Orden;
import com.GoTicket.Orden.Model.CheckoutRequest;

public interface OrdenService {

    List<Orden> listarOrdenes();

    Optional<Orden> buscarOrdenPorId(Long id);

    List<Orden> buscarOrdenesPorUsuario(Long idUsuario);

    Orden guardarOrden(Orden orden);

    Orden checkout(CheckoutRequest request);

    Orden actualizarOrden(Long id, Orden orden);

    void eliminarOrden(Long id);
}
