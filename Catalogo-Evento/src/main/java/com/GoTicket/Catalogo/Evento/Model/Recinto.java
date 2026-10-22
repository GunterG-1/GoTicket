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
@Table(name = "recinto", uniqueConstraints = @UniqueConstraint(
        name = "uk_recinto_nombre_ciudad", columnNames = {"nombre", "ciudad"}))
public class Recinto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recinto")
    private Long idRecinto;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "ciudad", nullable = false, length = 100)
    private String ciudad;

    @Column(name = "direccion", nullable = false, length = 250)
    private String direccion;

    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;
}
