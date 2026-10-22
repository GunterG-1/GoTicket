package com.GoTicket.Catalogo.Evento.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "usuario", uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuario_google_subject", columnNames = "google_subject"),
        @UniqueConstraint(name = "uk_usuario_email", columnNames = "email")
})
public class Usuario {
    @Id
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "google_subject", nullable = false, length = 255)
    private String googleSubject;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "rut", length = 12)
    private String rut;

    @Column(name = "telefono", length = 25)
    private String telefono;
}
