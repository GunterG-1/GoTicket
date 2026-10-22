package com.GoTicket.Catalogo.Evento.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sector_recinto", uniqueConstraints = @UniqueConstraint(
        name = "uk_sector_recinto_nombre", columnNames = {"id_recinto", "nombre"}))
public class SectorRecinto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sector_recinto")
    private Long idSectorRecinto;

    @Column(name = "id_recinto", nullable = false)
    private Long idRecinto;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;

    @Column(name = "modalidad", nullable = false, length = 20)
    private String modalidad;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "posicion_x", nullable = false)
    private Integer posicionX;

    @Column(name = "posicion_y", nullable = false)
    private Integer posicionY;
}
