package com.GoTicket.Catalogo.Evento.Repository;

import com.GoTicket.Catalogo.Evento.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByGoogleSubject(String googleSubject);
    Optional<Usuario> findByEmailIgnoreCase(String email);
}
