package com.GoTicket.Inventario.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.GoTicket.Inventario.Client.CatalogoEvento;
import com.GoTicket.Inventario.Client.CatalogoEventoClient;
import com.GoTicket.Inventario.Model.Inventario;
import com.GoTicket.Inventario.Repository.InventarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventarioSynchronizationService {

    private static final Logger logger = LoggerFactory.getLogger(InventarioSynchronizationService.class);

    private final CatalogoEventoClient catalogoEventoClient;
    private final InventarioRepository inventarioRepository;

    @Scheduled(
            initialDelayString = "${goticket.inventory.sync.initial-delay-ms:5000}",
            fixedDelayString = "${goticket.inventory.sync.fixed-delay-ms:30000}")
    @Transactional
    public void sincronizarEventos() {
        List<CatalogoEvento> eventos = catalogoEventoClient.listarEventos();
        if (eventos.isEmpty()) {
            logger.warn("No se pudieron sincronizar eventos desde Catalogo-Evento");
            return;
        }

        int creados = 0;
        for (CatalogoEvento evento : eventos) {
            if (evento.idEvento() == null || evento.capacidad() == null || evento.capacidad() <= 0) {
                logger.warn("Se omitió un evento de catálogo sin ID o capacidad válida: {}", evento);
                continue;
            }

            if (inventarioRepository.findByIdEvento(evento.idEvento()).isPresent()) {
                continue;
            }

            Inventario inventario = new Inventario();
            inventario.setIdEvento(evento.idEvento());
            inventario.setCantidadTotal(evento.capacidad());
            inventario.setCantidadDisponible(evento.capacidad());
            inventario.setActivo(true);
            inventario.setFechaActualizacion(LocalDateTime.now());
            inventarioRepository.save(inventario);
            creados++;
        }

        if (creados > 0) {
            logger.info("Se crearon {} inventarios para eventos existentes de Catalogo-Evento", creados);
        }
    }
}
