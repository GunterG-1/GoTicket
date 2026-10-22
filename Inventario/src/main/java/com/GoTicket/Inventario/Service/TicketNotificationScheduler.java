package com.GoTicket.Inventario.Service;

import com.GoTicket.Inventario.Model.NotificacionEntrada;
import com.GoTicket.Inventario.Repository.NotificacionEntradaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "goticket.mail.enabled", havingValue = "true")
public class TicketNotificationScheduler {
    private final NotificacionEntradaRepository notificationRepository;
    private final TicketNotificationService notificationService;

    @Scheduled(fixedDelayString = "${goticket.mail.retry-ms:60000}")
    public void procesarPendientes() {
        for (NotificacionEntrada notification :
                notificationRepository.findTop20ByEstadoAndProximoIntentoLessThanEqualOrderByIdNotificacionAsc(
                        "PENDIENTE", LocalDateTime.now())) {
            notificationService.enviarPendiente(notification.getIdNotificacion());
        }
    }
}
