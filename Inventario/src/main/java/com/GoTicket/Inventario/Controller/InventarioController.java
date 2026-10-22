package com.GoTicket.Inventario.Controller;

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

import com.GoTicket.Inventario.Model.Inventario;
import com.GoTicket.Inventario.Model.ConfirmarReservaRequest;
import com.GoTicket.Inventario.Model.CancelarReservaRequest;
import com.GoTicket.Inventario.Model.ReventaDetalles;
import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EntradaReservaRequest;
import com.GoTicket.Inventario.Model.PublicarReventaRequest;
import com.GoTicket.Inventario.Model.TransferirEntradaRequest;
import com.GoTicket.Inventario.Model.SectorDisponibilidad;
import com.GoTicket.Inventario.Service.InventarioService;
import com.GoTicket.Inventario.Service.TicketDocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventarios")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;
    private final TicketDocumentService ticketDocumentService;

    @GetMapping
    public ResponseEntity<List<Inventario>> listarInventarios() {
        return ResponseEntity.ok(inventarioService.listarInventarios());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Inventario> buscarInventarioPorId(@PathVariable Long id) {
        return inventarioService.buscarInventarioPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/evento/{idEvento}")
    public ResponseEntity<Inventario> buscarInventarioPorEvento(@PathVariable Long idEvento) {
        return inventarioService.buscarInventarioPorEvento(idEvento)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/evento/{idEvento}/sectores")
    public ResponseEntity<List<SectorDisponibilidad>> disponibilidadPorSector(@PathVariable Long idEvento) {
        return ResponseEntity.ok(inventarioService.disponibilidadPorSector(idEvento));
    }

    @PostMapping
    public ResponseEntity<Inventario> crearInventario(@RequestBody Inventario inventario) {
        Inventario inventarioGuardado = inventarioService.guardarInventario(inventario);
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioGuardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Inventario> actualizarInventario(@PathVariable Long id, @RequestBody Inventario inventario) {
        try {
            Inventario inventarioActualizado = inventarioService.actualizarInventario(id, inventario);
            return ResponseEntity.ok(inventarioActualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/evento/{idEvento}")
    public ResponseEntity<?> actualizarInventarioPorEvento(@PathVariable Long idEvento,
                                                            @RequestBody Inventario inventario) {
        try {
            return ResponseEntity.ok(inventarioService.actualizarInventarioPorEvento(idEvento, inventario));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", exception.getMessage()));
        } catch (RuntimeException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarInventario(@PathVariable Long id) {
        try {
            inventarioService.eliminarInventario(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/evento/{idEvento}")
    public ResponseEntity<?> eliminarInventarioPorEvento(@PathVariable Long idEvento) {
        try {
            inventarioService.eliminarInventarioPorEvento(idEvento);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(java.util.Map.of("error", exception.getMessage()));
        } catch (RuntimeException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/evento/{idEvento}/entradas")
    public ResponseEntity<List<Entrada>> listarEntradas(@PathVariable Long idEvento) {
        return ResponseEntity.ok(inventarioService.listarEntradas(idEvento));
    }

    @GetMapping("/usuario/{idUsuario}/entradas")
    public ResponseEntity<List<com.GoTicket.Inventario.Model.EntradaUsuarioResponse>> listarEntradasDeUsuario(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(inventarioService.listarEntradasDeUsuario(idUsuario));
    }

    @GetMapping("/usuario/{idUsuario}/entradas/{idEntrada}/pdf")
    public ResponseEntity<?> descargarEntradaPdf(@PathVariable Long idUsuario, @PathVariable Long idEntrada) {
        try {
            byte[] pdf = ticketDocumentService.descargar(idUsuario, idEntrada);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"goticket-entrada-" + idEntrada + ".pdf\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/acceso/validar")
    public ResponseEntity<?> validarAcceso(@RequestBody java.util.Map<String, String> request) {
        try {
            return ResponseEntity.ok(inventarioService.validarAcceso(request.get("codigoAcceso")));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", exception.getMessage()));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/reventa")
    public ResponseEntity<List<Entrada>> listarEntradasEnReventa() {
        return ResponseEntity.ok(inventarioService.listarEntradasEnReventa());
    }

    @GetMapping("/entradas/{idEntrada}/reventa")
    public ResponseEntity<ReventaDetalles> buscarEntradaEnReventa(@PathVariable Long idEntrada) {
        try {
            return ResponseEntity.ok(inventarioService.buscarEntradaEnReventa(idEntrada));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/reservas")
    public ResponseEntity<?> reservarEntradas(@RequestBody EntradaReservaRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.reservarEntradas(request));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/reservas/confirmar")
    public ResponseEntity<?> confirmarReserva(@RequestBody ConfirmarReservaRequest request) {
        try {
            inventarioService.confirmarReserva(request);
            return ResponseEntity.ok(java.util.Map.of("status", "CONFIRMADA"));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/reservas/{idReserva}")
    public ResponseEntity<?> buscarReserva(@PathVariable String idReserva) {
        try {
            return ResponseEntity.ok(inventarioService.buscarReserva(idReserva));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/reservas/cancelar")
    public ResponseEntity<?> cancelarReserva(@RequestBody CancelarReservaRequest request) {
        try {
            inventarioService.cancelarReserva(request);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/entradas/{idEntrada}/reventa")
    public ResponseEntity<?> publicarReventa(@PathVariable Long idEntrada, @RequestBody PublicarReventaRequest request) {
        try {
            return ResponseEntity.ok(inventarioService.publicarReventa(idEntrada, request));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/entradas/{idEntrada}/reventa/reservar")
    public ResponseEntity<?> reservarReventa(@PathVariable Long idEntrada, @RequestBody TransferirEntradaRequest request) {
        try {
            return ResponseEntity.ok(inventarioService.reservarReventa(idEntrada, request));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/entradas/{idEntrada}/reventa/cancelar")
    public ResponseEntity<?> cancelarReservaReventa(
            @PathVariable Long idEntrada, @RequestBody TransferirEntradaRequest request) {
        try {
            inventarioService.cancelarReservaReventa(idEntrada, request);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(java.util.Map.of("error", exception.getMessage()));
        }
    }

}
