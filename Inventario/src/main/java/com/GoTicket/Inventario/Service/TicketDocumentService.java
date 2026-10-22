package com.GoTicket.Inventario.Service;

import com.GoTicket.Inventario.Client.CatalogoEvento;
import com.GoTicket.Inventario.Client.CatalogoEventoClient;
import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EstadoEntrada;
import com.GoTicket.Inventario.Repository.EntradaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TicketDocumentService {
    private final EntradaRepository entryRepository;
    private final CatalogoEventoClient catalogoEventoClient;
    private final TicketPdfService ticketPdfService;

    @Transactional(readOnly = true)
    public byte[] descargar(Long idUsuario, Long idEntrada) {
        if (idUsuario == null || idEntrada == null) {
            throw new IllegalArgumentException("Usuario y entrada son obligatorios");
        }
        Entrada entry = entryRepository.findById(idEntrada)
                .orElseThrow(() -> new IllegalArgumentException("Entrada no encontrada"));
        if (!Objects.equals(idUsuario, entry.getIdUsuario())) {
            throw new IllegalArgumentException("Entrada no encontrada");
        }
        if (entry.getEstado() != EstadoEntrada.VENDIDO && entry.getEstado() != EstadoEntrada.USADA) {
            throw new IllegalStateException("El PDF solo está disponible para una entrada comprada");
        }
        CatalogoEvento event = catalogoEventoClient.buscarEvento(entry.getIdEvento())
                .orElseThrow(() -> new IllegalStateException("No se pudo cargar el evento de la entrada"));
        return ticketPdfService.generar(entry, event);
    }
}
