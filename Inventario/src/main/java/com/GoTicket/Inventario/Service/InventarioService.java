package com.GoTicket.Inventario.Service;

import java.util.List;
import java.util.Optional;

import com.GoTicket.Inventario.Model.Inventario;
import com.GoTicket.Inventario.Model.ConfirmarReservaRequest;
import com.GoTicket.Inventario.Model.CancelarReservaRequest;
import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EntradaReservaRequest;
import com.GoTicket.Inventario.Model.SectorDisponibilidad;
import com.GoTicket.Inventario.Model.PublicarReventaRequest;
import com.GoTicket.Inventario.Model.TransferirEntradaRequest;
import com.GoTicket.Inventario.Model.EntradaUsuarioResponse;
import com.GoTicket.Inventario.Model.ReservaDetalles;
import com.GoTicket.Inventario.Model.ReventaDetalles;

public interface InventarioService {

    List<Inventario> listarInventarios();

    Optional<Inventario> buscarInventarioPorId(Long id);

    Optional<Inventario> buscarInventarioPorEvento(Long idEvento);

    List<SectorDisponibilidad> disponibilidadPorSector(Long idEvento);

    Inventario guardarInventario(Inventario inventario);

    Inventario actualizarInventario(Long id, Inventario inventario);

    void eliminarInventario(Long id);

    Inventario actualizarInventarioPorEvento(Long idEvento, Inventario inventario);

    void eliminarInventarioPorEvento(Long idEvento);

    List<Entrada> listarEntradas(Long idEvento);

    List<EntradaUsuarioResponse> listarEntradasDeUsuario(Long idUsuario);

    Entrada validarAcceso(String codigoAcceso);

    List<Entrada> listarEntradasEnReventa();

    ReventaDetalles buscarEntradaEnReventa(Long idEntrada);

    List<Entrada> reservarEntradas(EntradaReservaRequest request);

    void confirmarReserva(ConfirmarReservaRequest request);

    void cancelarReserva(CancelarReservaRequest request);

    ReservaDetalles buscarReserva(String idReserva);

    Entrada publicarReventa(Long idEntrada, PublicarReventaRequest request);

    Entrada reservarReventa(Long idEntrada, TransferirEntradaRequest request);

    void cancelarReservaReventa(Long idEntrada, TransferirEntradaRequest request);

}
