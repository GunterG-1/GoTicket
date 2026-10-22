package com.GoTicket.Inventario.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "entrada")
public class Entrada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrada")
    private Long idEntrada;

    @Column(name = "id_evento", nullable = false)
    private Long idEvento;

    @Column(name = "sector", nullable = false, length = 80)
    private String sector;

    @Column(name = "numero_asiento", nullable = false, length = 40)
    private String numeroAsiento;

    @Column(name = "precio", precision = 10, scale = 2, nullable = false)
    private BigDecimal precio;

    @Column(name = "precio_original", precision = 10, scale = 2)
    private BigDecimal precioOriginal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoEntrada estado = EstadoEntrada.DISPONIBLE;

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "id_orden")
    private Long idOrden;

    @Column(name = "id_reserva", length = 36)
    private String idReserva;

    @Column(name = "id_usuario_reserva")
    private Long idUsuarioReserva;

    @Column(name = "fecha_bloqueo")
    private LocalDateTime fechaBloqueo;

    @Column(name = "expira_en")
    private LocalDateTime expiraEn;

    @JsonIgnore
    @Column(name = "codigo_acceso", unique = true, length = 36)
    private String codigoAcceso;

    @Column(name = "fecha_uso")
    private LocalDateTime fechaUso;
}