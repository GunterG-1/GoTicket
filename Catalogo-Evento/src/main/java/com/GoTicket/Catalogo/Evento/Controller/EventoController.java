package com.GoTicket.Catalogo.Evento.Controller;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.GoTicket.Catalogo.Evento.Model.Evento;
import com.GoTicket.Catalogo.Evento.Model.SectorEvento;
import com.GoTicket.Catalogo.Evento.Model.SectorEventoRequest;
import com.GoTicket.Catalogo.Evento.Service.EventoService;
import com.GoTicket.Catalogo.Evento.Service.SectorEventoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService eventoService;
    private final SectorEventoService sectorEventoService;

    @GetMapping
    public ResponseEntity<List<Evento>> listarEventos() {
        return ResponseEntity.ok(eventoService.listarEventos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Evento> buscarEventoPorId(@PathVariable Long id) {
        return eventoService.buscarEventoPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/sectores")
    public ResponseEntity<?> listarSectores(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(sectorEventoService.listar(id));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/sectores")
    public ResponseEntity<?> configurarSectores(@PathVariable Long id,
            @RequestBody List<SectorEventoRequest> requests) {
        try {
            List<SectorEvento> sectors = sectorEventoService.configurar(id, requests);
            return ResponseEntity.ok(sectors);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<?> buscarEventos(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) LocalDateTime desde,
            @RequestParam(required = false) LocalDateTime hasta,
            @RequestParam(required = false) BigDecimal precioMinimo,
            @RequestParam(required = false) BigDecimal precioMaximo,
            @PageableDefault(size = 12, sort = "fechaInicio", direction = Sort.Direction.ASC) Pageable pageable) {
        try {
            Page<Evento> results = eventoService.buscarEventos(texto, categoria, ciudad,
                    desde, hasta, precioMinimo, precioMaximo, pageable);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> crearEvento(@RequestBody Evento evento) {
        try {
            Evento eventoGuardado = eventoService.guardarEvento(evento);
            return ResponseEntity.status(HttpStatus.CREATED).body(eventoGuardado);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Evento> actualizarEvento(@PathVariable Long id, @RequestBody Evento evento) {
        try {
            Evento eventoActualizado = eventoService.actualizarEvento(id, evento);
            return ResponseEntity.ok(eventoActualizado);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEvento(@PathVariable Long id) {
        try {
            eventoService.eliminarEvento(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
