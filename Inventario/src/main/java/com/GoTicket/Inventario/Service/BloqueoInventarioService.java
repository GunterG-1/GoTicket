package com.GoTicket.Inventario.Service;

import com.GoTicket.Inventario.Model.BloqueoInventario;
import com.GoTicket.Inventario.Model.Inventario;
import com.GoTicket.Inventario.Repository.BloqueoInventarioRepository;
import com.GoTicket.Inventario.Repository.InventarioRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BloqueoInventarioService {
    private final BloqueoInventarioRepository bloqueoRepository;
    private final InventarioRepository inventarioRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void liberarExpirados() {
        bloqueoRepository.findByResueltoFalseAndExpiraEnBefore(LocalDateTime.now()).forEach(this::liberar);
    }

    @Transactional
    public void liberar(BloqueoInventario bloqueo) {
        inventarioRepository.findByIdEvento(bloqueo.getIdEvento()).ifPresent(inventory -> {
            int available = inventory.getCantidadDisponible() == null ? 0 : inventory.getCantidadDisponible();
            int total = inventory.getCantidadTotal() == null ? Integer.MAX_VALUE : inventory.getCantidadTotal();
            inventory.setCantidadDisponible(Math.min(total, available + bloqueo.getCantidad()));
            inventory.setFechaActualizacion(LocalDateTime.now());
            inventarioRepository.save(inventory);
        });
        bloqueo.setResuelto(true);
        bloqueoRepository.save(bloqueo);
    }

    public void create(BloqueoInventario bloqueo) {
        bloqueoRepository.save(bloqueo);
    }
}