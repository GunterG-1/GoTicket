package com.GoTicket.Catalogo.Evento.Controller;

import com.GoTicket.Catalogo.Evento.Model.Recinto;
import com.GoTicket.Catalogo.Evento.Model.SectorRecintoRequest;
import com.GoTicket.Catalogo.Evento.Service.RecintoService;
import com.GoTicket.Catalogo.Evento.Service.SectorRecintoService;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@RestController
@RequestMapping("/api/recintos")
@RequiredArgsConstructor
public class RecintoController {
    private final RecintoService recintoService;
    private final SectorRecintoService sectorRecintoService;

    @GetMapping
    public ResponseEntity<List<Recinto>> listar() {
        return ResponseEntity.ok(recintoService.listar());
    }

    @GetMapping("/{id}/sectores")
    public ResponseEntity<?> listarSectores(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(sectorRecintoService.listar(id));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/sectores")
    public ResponseEntity<?> configurarSectores(@PathVariable Long id,
            @RequestBody List<SectorRecintoRequest> requests) {
        try {
            return ResponseEntity.ok(sectorRecintoService.configurar(id, requests));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Recinto recinto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(recintoService.guardar(recinto));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Recinto recinto) {
        try {
            return ResponseEntity.ok(recintoService.actualizar(id, recinto));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            recintoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("error", exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }
}
