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
import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EntradaReservaRequest;
import com.GoTicket.Inventario.Model.PublicarReventaRequest;
import com.GoTicket.Inventario.Model.TransferirEntradaRequest;
import com.GoTicket.Inventario.Service.InventarioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventarios")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarInventario(@PathVariable Long id) {
        try {
            inventarioService.eliminarInventario(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/evento/{idEvento}/entradas")
    public ResponseEntity<List<Entrada>> listarEntradas(@PathVariable Long idEvento) {
        return ResponseEntity.ok(inventarioService.listarEntradas(idEvento));
    }

    @GetMapping("/usuario/{idUsuario}/entradas")
    public ResponseEntity<List<Entrada>> listarEntradasDeUsuario(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(inventarioService.listarEntradasDeUsuario(idUsuario));
    }

    @GetMapping("/reventa")
    public ResponseEntity<List<Entrada>> listarEntradasEnReventa() {
        return ResponseEntity.ok(inventarioService.listarEntradasEnReventa());
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

    @PostMapping("/entradas/{idEntrada}/reventa")
    public ResponseEntity<?> publicarReventa(@PathVariable Long idEntrada, @RequestBody PublicarReventaRequest request) {
        try {
            return ResponseEntity.ok(inventarioService.publicarReventa(idEntrada, request));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/entradas/{idEntrada}/transferir")
    public ResponseEntity<?> transferirEntrada(@PathVariable Long idEntrada, @RequestBody TransferirEntradaRequest request) {
        try {
            return ResponseEntity.ok(inventarioService.transferirEntrada(idEntrada, request));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", exception.getMessage()));
        }
    }
}
