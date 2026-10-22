package com.GoTicket.Catalogo.Evento.Service;

import com.GoTicket.Catalogo.Evento.Model.ActualizarPerfilRequest;
import com.GoTicket.Catalogo.Evento.Model.IdentidadGoogleRequest;
import com.GoTicket.Catalogo.Evento.Model.Usuario;
import com.GoTicket.Catalogo.Evento.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public Usuario sincronizarIdentidadGoogle(IdentidadGoogleRequest request) {
        validarIdentidad(request);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByGoogleSubject(request.googleSubject()).orElse(null);

        if (usuario == null) {
            usuario = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
        }
        if (usuario != null) {
            Usuario emailOwner = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
            if (emailOwner != null && !emailOwner.getIdUsuario().equals(usuario.getIdUsuario())) {
                throw new IllegalStateException("El correo ya está asociado a otro perfil");
            }
            usuario.setGoogleSubject(request.googleSubject());
            usuario.setEmail(email);
            usuario.setNombre(request.nombre().trim());
            return usuarioRepository.save(usuario);
        }

        Usuario idOwner = usuarioRepository.findById(request.idUsuario()).orElse(null);
        if (idOwner != null) {
            throw new IllegalStateException("No se puede vincular esta cuenta al ID histórico de otro perfil");
        }
        Usuario created = new Usuario(request.idUsuario(), request.googleSubject(), email,
                request.nombre().trim(), null, null);
        return usuarioRepository.save(created);
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Perfil no encontrado"));
    }

    @Transactional
    public Usuario actualizarPerfil(Long idUsuario, ActualizarPerfilRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos del perfil son obligatorios");
        }
        Usuario usuario = obtener(idUsuario);
        usuario.setRut(normalizarRut(request.rut()));
        usuario.setTelefono(normalizarTelefono(request.telefono()));
        return usuarioRepository.save(usuario);
    }

    private void validarIdentidad(IdentidadGoogleRequest request) {
        if (request == null || request.idUsuario() == null
                || request.googleSubject() == null || request.googleSubject().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("La identidad verificada de Google está incompleta");
        }
    }

    private String normalizarRut(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String rut = value.replace(".", "").replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
        if (!rut.matches("\\d{1,8}[0-9K]")) {
            throw new IllegalArgumentException("El RUT debe tener un formato válido");
        }
        String body = rut.substring(0, rut.length() - 1);
        int sum = 0;
        int factor = 2;
        for (int index = body.length() - 1; index >= 0; index--) {
            sum += Character.digit(body.charAt(index), 10) * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int remainder = 11 - sum % 11;
        char verifier = remainder == 11 ? '0' : remainder == 10 ? 'K' : (char) ('0' + remainder);
        if (rut.charAt(rut.length() - 1) != verifier) {
            throw new IllegalArgumentException("El dígito verificador del RUT no es válido");
        }
        return body + "-" + verifier;
    }

    private String normalizarTelefono(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String phone = value.trim();
        if (!phone.matches("^[+0-9][0-9+() -]{5,24}$")) {
            throw new IllegalArgumentException("El teléfono tiene un formato no válido");
        }
        return phone;
    }
}
