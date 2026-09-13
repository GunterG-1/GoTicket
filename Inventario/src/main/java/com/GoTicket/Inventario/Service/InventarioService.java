package com.GoTicket.Inventario.Service;

import java.util.List;
import java.util.Optional;

import com.GoTicket.Inventario.Model.Inventario;
import com.GoTicket.Inventario.Model.ConfirmarReservaRequest;
import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EntradaReservaRequest;
import com.GoTicket.Inventario.Model.PublicarReventaRequest;
import com.GoTicket.Inventario.Model.TransferirEntradaRequest;

public interface InventarioService {

    List<Inventario> listarInventarios();

    Optional<Inventario> buscarInventarioPorId(Long id);

    Optional<Inventario> buscarInventarioPorEvento(Long idEvento);

    Inventario guardarInventario(Inventario inventario);

    Inventario actualizarInventario(Long id, Inventario inventario);

    void eliminarInventario(Long id);

    List<Entrada> listarEntradas(Long idEvento);

    List<Entrada> listarEntradasDeUsuario(Long idUsuario);

    List<Entrada> listarEntradasEnReventa();

    List<Entrada> reservarEntradas(EntradaReservaRequest request);

    void confirmarReserva(ConfirmarReservaRequest request);

    Entrada publicarReventa(Long idEntrada, PublicarReventaRequest request);

    Entrada transferirEntrada(Long idEntrada, TransferirEntradaRequest request);
}
