package com.GoTicket.Catalogo.Evento.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.GoTicket.Catalogo.Evento.Model.Evento;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {

	Optional<Evento> findByNombre(String nombre);
}
