package com.GoTicket.Pagos.Service;

import java.util.List;
import java.util.Optional;

import com.GoTicket.Pagos.Model.Pago;

public interface PagoService {

    List<Pago> listarPagos();

    Optional<Pago> buscarPagoPorId(Long id);

    List<Pago> buscarPagosPorUsuario(Long idUsuario);

    List<Pago> buscarPagosPorOrden(Long idOrden);

    Pago guardarPago(Pago pago);

    Pago procesarPago(Long id, String numeroTarjeta, String vencimiento, String cvv, String titular);

    Pago actualizarPago(Long id, Pago pago);

    void eliminarPago(Long id);
}
