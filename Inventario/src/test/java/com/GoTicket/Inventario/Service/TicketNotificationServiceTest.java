package com.GoTicket.Inventario.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.GoTicket.Inventario.Client.CatalogoEvento;
import com.GoTicket.Inventario.Client.CatalogoEventoClient;
import com.GoTicket.Inventario.Client.CatalogoUsuario;
import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EstadoEntrada;
import com.GoTicket.Inventario.Model.NotificacionEntrada;
import com.GoTicket.Inventario.Repository.EntradaRepository;
import com.GoTicket.Inventario.Repository.NotificacionEntradaRepository;

@ExtendWith(MockitoExtension.class)
class TicketNotificationServiceTest {
    @Mock private NotificacionEntradaRepository notificationRepository;
    @Mock private EntradaRepository entryRepository;
    @Mock private CatalogoEventoClient catalogoEventoClient;
    @Mock private TicketPdfService ticketPdfService;
    @Mock private JavaMailSender mailSender;

    private TicketNotificationService service;
    private NotificacionEntrada notification;
    private Entrada entry;
    private CatalogoEvento event;
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        service = new TicketNotificationService(notificationRepository, entryRepository,
                catalogoEventoClient, ticketPdfService, mailSender);
        ReflectionTestUtils.setField(service, "from", "noreply@example.com");
        notification = new NotificacionEntrada(15L, 99L, 7L, "PENDIENTE",
                0, LocalDateTime.now().minusMinutes(1), null, null);
        entry = new Entrada();
        entry.setIdEntrada(21L);
        entry.setIdEvento(10L);
        entry.setIdOrden(99L);
        entry.setIdUsuario(7L);
        entry.setSector("General");
        entry.setNumeroAsiento("1");
        entry.setPrecio(BigDecimal.TEN);
        entry.setEstado(EstadoEntrada.VENDIDO);
        entry.setCodigoAcceso("unique-qr-code");
        event = new CatalogoEvento(10L, 100, BigDecimal.TEN, "Evento", "musica",
                "Recinto", LocalDateTime.now().plusDays(1), true);
        mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(notificationRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(notification));
        when(catalogoEventoClient.buscarUsuario(7L))
                .thenReturn(new CatalogoUsuario(7L, "Cliente", "cliente@example.com", null, null));
        when(entryRepository.findByIdOrden(99L)).thenReturn(List.of(entry));
        when(catalogoEventoClient.buscarEvento(10L)).thenReturn(Optional.of(event));
        when(ticketPdfService.generar(entry, event)).thenReturn("%PDF-test".getBytes());
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void marcaLaNotificacionEnviadaCuandoElCorreoSaleCorrectamente() {
        when(notificationRepository.save(any(NotificacionEntrada.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.enviarPendiente(15L);

        assertEquals("ENVIADO", notification.getEstado());
        assertNotNull(notification.getFechaEnvio());
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void conservaElPendienteYProgramaReintentoSiFallaElServidorDeCorreo() {
        doThrow(new MailSendException("SMTP no disponible")).when(mailSender).send(any(MimeMessage.class));
        when(notificationRepository.save(any(NotificacionEntrada.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.enviarPendiente(15L);

        assertEquals("PENDIENTE", notification.getEstado());
        assertEquals(1, notification.getIntentos());
        assertTrue(notification.getProximoIntento().isAfter(LocalDateTime.now()));
        assertEquals("SMTP no disponible", notification.getUltimoError());
    }
}
