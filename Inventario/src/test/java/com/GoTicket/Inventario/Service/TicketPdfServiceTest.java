package com.GoTicket.Inventario.Service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import com.GoTicket.Inventario.Client.CatalogoEvento;
import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EstadoEntrada;

class TicketPdfServiceTest {
    private final TicketPdfService service = new TicketPdfService();

    @Test
    void generaPdfLegibleConDatosDelEventoYCodigoQr() throws Exception {
        Entrada ticket = new Entrada();
        ticket.setIdEntrada(21L);
        ticket.setIdEvento(8L);
        ticket.setSector("Platea");
        ticket.setNumeroAsiento("A-21");
        ticket.setPrecio(new BigDecimal("15000.00"));
        ticket.setEstado(EstadoEntrada.VENDIDO);
        ticket.setIdOrden(99L);
        ticket.setCodigoAcceso("codigo-qr-unico");
        CatalogoEvento event = new CatalogoEvento(8L, 200,
                new BigDecimal("15000.00"), "Concierto de prueba", "musica", "Teatro Central",
                LocalDateTime.of(2027, 3, 12, 20, 30), true);

        byte[] pdf = service.generar(ticket, event);

        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Concierto de prueba"));
            assertTrue(text.contains("codigo QR"));
            assertTrue(text.contains("Folio: A-21"));
        }
    }
}
