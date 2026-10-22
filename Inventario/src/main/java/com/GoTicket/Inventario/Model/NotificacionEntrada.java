package com.GoTicket.Inventario.Model;

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

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notificacion_entrada", uniqueConstraints = @UniqueConstraint(
        name = "uk_notificacion_entrada_orden", columnNames = "id_orden"))
public class NotificacionEntrada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long idNotificacion;

    @Column(name = "id_orden", nullable = false)
    private Long idOrden;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "intentos", nullable = false)
    private int intentos;

    @Column(name = "proximo_intento", nullable = false)
    private LocalDateTime proximoIntento;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "ultimo_error", length = 1000)
    private String ultimoError;
}
