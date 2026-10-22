package com.GoTicket.Catalogo.Evento.Controller;

import com.GoTicket.Catalogo.Evento.Model.ActualizarPerfilRequest;
import com.GoTicket.Catalogo.Evento.Model.IdentidadGoogleRequest;
import com.GoTicket.Catalogo.Evento.Model.Usuario;
import com.GoTicket.Catalogo.Evento.Model.UsuarioPerfilResponse;
import com.GoTicket.Catalogo.Evento.Service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    @PostMapping("/google")
    public ResponseEntity<?> sincronizarIdentidad(@RequestBody IdentidadGoogleRequest request) {
        try {
            return ResponseEntity.ok(respuesta(usuarioService.sincronizarIdentidadGoogle(request)));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", exception.getMessage()));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioPerfilResponse> obtenerPerfil(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(respuesta(usuarioService.obtener(id)));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/perfil")
    public ResponseEntity<?> actualizarPerfil(@PathVariable Long id,
            @RequestBody ActualizarPerfilRequest request) {
        try {
            return ResponseEntity.ok(respuesta(usuarioService.actualizarPerfil(id, request)));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    private UsuarioPerfilResponse respuesta(Usuario usuario) {
        return new UsuarioPerfilResponse(usuario.getIdUsuario(), usuario.getNombre(),
                usuario.getEmail(), usuario.getRut(), usuario.getTelefono());
    }
}
