package com.GoTicket.Inventario.Service;

import com.GoTicket.Inventario.Client.CatalogoEvento;
import com.GoTicket.Inventario.Client.CatalogoEventoClient;
import com.GoTicket.Inventario.Client.CatalogoUsuario;
import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EstadoEntrada;
import com.GoTicket.Inventario.Model.NotificacionEntrada;
import com.GoTicket.Inventario.Repository.EntradaRepository;
import com.GoTicket.Inventario.Repository.NotificacionEntradaRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketNotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketNotificationService.class);
    private static final String PENDING = "PENDIENTE";
    private static final String SENT = "ENVIADO";
    private static final String FAILED = "FALLIDO";
    private static final int MAX_ATTEMPTS = 10;

    private final NotificacionEntradaRepository notificationRepository;
    private final EntradaRepository entryRepository;
    private final CatalogoEventoClient catalogoEventoClient;
    private final TicketPdfService ticketPdfService;
    private final JavaMailSender mailSender;

    @Value("${goticket.mail.from:noreply@goticket.local}")
    private String from;

    @Transactional
    public void programar(Long idOrden, Long idUsuario) {
        if (idOrden == null || idUsuario == null) {
            throw new IllegalArgumentException("Orden y usuario son obligatorios para notificar la compra");
        }
        if (notificationRepository.findByIdOrden(idOrden).isPresent()) {
            return;
        }
        notificationRepository.save(new NotificacionEntrada(null, idOrden, idUsuario,
                PENDING, 0, LocalDateTime.now(), null, null));
    }

    @Transactional
    public void enviarPendiente(Long idNotificacion) {
        NotificacionEntrada notification = notificationRepository.findByIdForUpdate(idNotificacion).orElse(null);
        if (notification == null || !PENDING.equals(notification.getEstado())
                || notification.getProximoIntento().isAfter(LocalDateTime.now())) {
            return;
        }

        try {
            enviar(notification);
            notification.setEstado(SENT);
            notification.setFechaEnvio(LocalDateTime.now());
            notification.setUltimoError(null);
        } catch (Exception exception) {
            notification.setIntentos(notification.getIntentos() + 1);
            if (notification.getIntentos() >= MAX_ATTEMPTS) {
                notification.setEstado(FAILED);
            }
            long retryMinutes = Math.min(60, 1L << Math.min(notification.getIntentos(), 5));
            notification.setProximoIntento(LocalDateTime.now().plusMinutes(retryMinutes));
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            notification.setUltimoError(message.substring(0, Math.min(message.length(), 1000)));
            LOGGER.warn("No se pudo enviar la notificacion de entradas para orden {}. Reintento {} en {} minutos.",
                    notification.getIdOrden(), notification.getIntentos(), retryMinutes, exception);
        }
        notificationRepository.save(notification);
    }

    private void enviar(NotificacionEntrada notification) throws Exception {
        CatalogoUsuario user = catalogoEventoClient.buscarUsuario(notification.getIdUsuario());
        if (user.email() == null || user.email().isBlank()) {
            throw new IllegalStateException("El perfil no tiene un correo de confirmacion");
        }
        List<Entrada> entries = entryRepository.findByIdOrden(notification.getIdOrden()).stream()
                .filter(entry -> EstadoEntrada.VENDIDO.equals(entry.getEstado())
                        || EstadoEntrada.USADA.equals(entry.getEstado()))
                .toList();
        if (entries.isEmpty()) {
            throw new IllegalStateException("La orden aun no tiene entradas vendidas");
        }
        CatalogoEvento event = catalogoEventoClient.buscarEvento(entries.get(0).getIdEvento())
                .orElseThrow(() -> new IllegalStateException("No se pudo cargar el evento de las entradas"));
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(user.email());
        helper.setSubject("Tus entradas GoTicket - " + event.nombre());
        helper.setText("Hola " + user.nombre() + ",\n\nAdjuntamos tus entradas digitales en PDF. "
                + "Cada archivo contiene un codigo QR unico y de un solo uso.\n\nGoTicket", false);
        for (Entrada entry : entries) {
            byte[] pdf = ticketPdfService.generar(entry, event);
            helper.addAttachment("goticket-entrada-" + entry.getIdEntrada() + ".pdf",
                    new ByteArrayResource(pdf), "application/pdf");
        }
        mailSender.send(message);
    }
}
