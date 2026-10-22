package com.GoTicket.Catalogo.Evento.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.GoTicket.Catalogo.Evento.Model.ActualizarPerfilRequest;
import com.GoTicket.Catalogo.Evento.Model.IdentidadGoogleRequest;
import com.GoTicket.Catalogo.Evento.Model.Usuario;
import com.GoTicket.Catalogo.Evento.Repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {
    @Mock private UsuarioRepository usuarioRepository;

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarioRepository);
    }

    @Test
    void vinculaCuentaGoogleAlIdHistoricoYGuardaPerfil() {
        when(usuarioRepository.findByGoogleSubject("google-sub")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(123L)).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario user = service.sincronizarIdentidadGoogle(
                new IdentidadGoogleRequest(123L, "google-sub", "User@Example.com", "Usuario"));

        assertEquals(123L, user.getIdUsuario());
        assertEquals("user@example.com", user.getEmail());
        assertEquals("google-sub", user.getGoogleSubject());
    }

    @Test
    void mantieneElIdAlCambiarElCorreoDeUnaCuentaGoogleVinculada() {
        Usuario user = new Usuario(123L, "google-sub", "old@example.com", "Nombre anterior", null, null);
        when(usuarioRepository.findByGoogleSubject("google-sub")).thenReturn(Optional.of(user));
        when(usuarioRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(user)).thenReturn(user);

        Usuario updated = service.sincronizarIdentidadGoogle(
                new IdentidadGoogleRequest(456L, "google-sub", "new@example.com", "Nombre nuevo"));

        assertEquals(123L, updated.getIdUsuario());
        assertEquals("new@example.com", updated.getEmail());
    }

    @Test
    void normalizaRutValidoYRechazaDigitoVerificadorIncorrecto() {
        Usuario user = new Usuario(123L, "sub", "user@example.com", "Usuario", null, null);
        when(usuarioRepository.findById(123L)).thenReturn(Optional.of(user));
        when(usuarioRepository.save(user)).thenReturn(user);

        service.actualizarPerfil(123L, new ActualizarPerfilRequest("12.345.678-5", "+56 9 1234 5678"));

        assertEquals("12345678-5", user.getRut());
        assertEquals("+56 9 1234 5678", user.getTelefono());
        verify(usuarioRepository).save(user);
        assertThrows(IllegalArgumentException.class, () -> service.actualizarPerfil(
                123L, new ActualizarPerfilRequest("12.345.678-0", "123456789")));
    }
}
