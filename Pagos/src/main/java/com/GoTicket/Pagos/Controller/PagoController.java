package com.GoTicket.Pagos.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.GoTicket.Pagos.Model.Pago;
import com.GoTicket.Pagos.Service.PagoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @GetMapping
    public ResponseEntity<List<Pago>> listarPagos() {
        return ResponseEntity.ok(pagoService.listarPagos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pago> buscarPagoPorId(@PathVariable Long id) {
        return pagoService.buscarPagoPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Pago>> buscarPagosPorUsuario(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(pagoService.buscarPagosPorUsuario(idUsuario));
    }

    @GetMapping("/orden/{idOrden}")
    public ResponseEntity<List<Pago>> buscarPagosPorOrden(@PathVariable Long idOrden) {
        return ResponseEntity.ok(pagoService.buscarPagosPorOrden(idOrden));
    }

    @PostMapping
    public ResponseEntity<?> crearPago(@RequestBody Pago pago) {
        try {
            Pago pagoGuardado = pagoService.guardarPago(pago);
            return ResponseEntity.status(HttpStatus.CREATED).body(pagoGuardado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/procesar")
    public ResponseEntity<?> procesarPago(@PathVariable Long id, @RequestBody ProcesarPagoRequest solicitud) {
        try {
            return ResponseEntity.ok(pagoService.procesarPago(id, solicitud.numeroTarjeta(), solicitud.vencimiento(),
                    solicitud.cvv(), solicitud.titular()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pago> actualizarPago(@PathVariable Long id, @RequestBody Pago pago) {
        try {
            Pago pagoActualizado = pagoService.actualizarPago(id, pago);
            return ResponseEntity.ok(pagoActualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPago(@PathVariable Long id) {
        try {
            pagoService.eliminarPago(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
