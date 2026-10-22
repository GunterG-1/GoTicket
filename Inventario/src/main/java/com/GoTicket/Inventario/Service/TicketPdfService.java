package com.GoTicket.Inventario.Service;

import com.GoTicket.Inventario.Client.CatalogoEvento;
import com.GoTicket.Inventario.Model.Entrada;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;

@Service
public class TicketPdfService {
    public byte[] generar(Entrada ticket, CatalogoEvento event) {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream pdfOutput = new ByteArrayOutputStream();
                ByteArrayOutputStream qrOutput = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                write(content, bold, 22, 48, 742, "GoTicket - Entrada digital");
                write(content, bold, 16, 48, 704, safe(event.nombre()));
                write(content, regular, 11, 48, 678, "Lugar: " + safe(event.lugar()));
                String date = event.fechaInicio() == null ? "Fecha por confirmar"
                        : event.fechaInicio().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
                write(content, regular, 11, 48, 658, "Fecha: " + date);
                write(content, regular, 11, 48, 638, "Sector: " + safe(ticket.getSector()));
                write(content, regular, 11, 48, 618, "Entrada: #" + ticket.getIdEntrada()
                        + "  Folio: " + safe(ticket.getNumeroAsiento()));
                write(content, regular, 11, 48, 598, "Orden: #" + ticket.getIdOrden());
                write(content, regular, 11, 48, 578, "Precio: CLP " + ticket.getPrecio());
                write(content, bold, 12, 48, 538, "Estado: " + ticket.getEstado().name());

                if (ticket.getCodigoAcceso() != null && !ticket.getCodigoAcceso().isBlank()) {
                    var matrix = new QRCodeWriter().encode(ticket.getCodigoAcceso(),
                            BarcodeFormat.QR_CODE, 280, 280);
                    MatrixToImageWriter.writeToStream(matrix, "PNG", qrOutput);
                    PDImageXObject qrImage = LosslessFactory.createFromImage(document,
                            javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(qrOutput.toByteArray())));
                    content.drawImage(qrImage, 166, 230, 280, 280);
                    write(content, regular, 9, 48, 202, "Presenta este codigo QR en el acceso. Es personal y de un solo uso.");
                } else {
                    write(content, bold, 12, 48, 470, "Esta entrada ya fue utilizada; el codigo QR fue invalidado.");
                }
                write(content, regular, 9, 48, 176, "Conserva esta entrada y no compartas el codigo antes del evento.");
            }
            document.save(pdfOutput);
            return pdfOutput.toByteArray();
        } catch (IOException | WriterException exception) {
            throw new IllegalStateException("No se pudo generar el PDF de la entrada", exception);
        }
    }

    private void write(PDPageContentStream content, PDType1Font font, int fontSize,
            float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(safe(text));
        content.endText();
    }

    private String safe(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\x20-\\x7E]", "");
    }
}
