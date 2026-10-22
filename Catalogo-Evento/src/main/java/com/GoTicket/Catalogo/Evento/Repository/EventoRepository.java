package com.GoTicket.Catalogo.Evento.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.GoTicket.Catalogo.Evento.Model.Evento;
import java.util.List;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {

	Optional<Evento> findByNombre(String nombre);
	boolean existsByIdRecinto(Long idRecinto);
	List<Evento> findByIdRecinto(Long idRecinto);

	@Query("""
			select e from Evento e
			where e.activo = true
			  and (:texto is null or lower(e.nombre) like lower(concat(concat('%', :texto), '%'))
			       or lower(e.descripcion) like lower(concat(concat('%', :texto), '%'))
			       or lower(e.categoria) like lower(concat(concat('%', :texto), '%')))
			  and (
			       :categoria is null
			       or lower(e.categoria) like lower(concat(concat('%', :categoria), '%'))
			       or (
			           lower(:categoria) in ('musica', 'música')
			           and (
			               lower(e.categoria) like '%pop%'
			               or lower(e.categoria) like '%rock%'
			               or lower(e.categoria) like '%urbano%'
			               or lower(e.categoria) like '%bachata%'
			               or lower(e.categoria) like '%bolero%'
			               or lower(e.categoria) like '%balada%'
			               or lower(e.categoria) like '%electr%'
			               or lower(e.categoria) like '%reggaet%'
			               or lower(e.categoria) like '%jazz%'
			               or lower(e.categoria) like '%metal%'
			               or lower(e.categoria) like '%romant%'
			               or lower(e.categoria) like '%fan meeting%'
			               or lower(e.categoria) like '%fiesta%'
			               or lower(e.categoria) like '%festival%'
			               or lower(e.categoria) like '%techno%'
			           )
			       )
			       or (
			           lower(:categoria) in ('deporte', 'deportes')
			           and (
			               lower(e.categoria) like '%deport%'
			               or lower(e.categoria) like '%fut%'
			               or lower(e.categoria) like '%corrida%'
			               or lower(e.categoria) like '%exhibici%'
			           )
			       )
			       or (
			           lower(:categoria) = 'teatro'
			           and (
			               lower(e.categoria) like '%teatro%'
			               or lower(e.categoria) like '%humor%'
			               or lower(e.categoria) like '%circo%'
			           )
			       )
			       or (
			           lower(:categoria) = 'familia'
			           and (
			               lower(e.categoria) like '%familia%'
			               or lower(e.categoria) like '%infantil%'
			               or lower(e.categoria) like '%youtuber%'
			               or lower(e.categoria) like '%circo%'
			           )
			       )
			       or (
			           lower(:categoria) = 'especiales'
			           and (
			               lower(e.categoria) like '%especial%'
			               or lower(e.categoria) like '%fonda%'
			               or lower(e.categoria) like '%festival%'
			               or lower(e.categoria) like '%homenaje%'
			               or lower(e.categoria) like '%exhibici%'
			           )
			       )
			  )
			  and (:ciudad is null or lower(e.lugar) like lower(concat(concat('%', :ciudad), '%')))
			  and (:desde is null or e.fechaInicio >= :desde)
			  and (:hasta is null or e.fechaInicio <= :hasta)
			  and (:precioMinimo is null or e.precio >= :precioMinimo)
			  and (:precioMaximo is null or e.precio <= :precioMaximo)
			""")
	Page<Evento> buscarEventos(
			@Param("texto") String texto,
			@Param("categoria") String categoria,
			@Param("ciudad") String ciudad,
			@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta,
			@Param("precioMinimo") BigDecimal precioMinimo,
			@Param("precioMaximo") BigDecimal precioMaximo,
			Pageable pageable);
}
