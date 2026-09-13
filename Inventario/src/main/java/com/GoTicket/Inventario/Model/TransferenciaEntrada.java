package com.GoTicket.Inventario.Model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transferencia_entrada")
public class TransferenciaEntrada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTransferencia;

    @Column(nullable = false)
    private Long idEntrada;

    @Column(nullable = false)
    private Long idUsuarioOrigen;

    @Column(nullable = false)
    private Long idUsuarioDestino;

    @Column(nullable = false)
    private Long idOrden;

    @Column(nullable = false)
    private LocalDateTime fecha;
}