package com.GoTicket.Inventario.Model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bloqueo_inventario")
public class BloqueoInventario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idBloqueo;
    @Column(nullable = false, unique = true)
    private Long idOrden;
    @Column(nullable = false)
    private Long idEvento;
    @Column(nullable = false)
    private Integer cantidad;
    @Column(nullable = false)
    private LocalDateTime expiraEn;
    @Column(nullable = false)
    private boolean resuelto;
}