package com.GoTicket.Catalogo.Evento.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.GoTicket.Catalogo.Evento.Model.Evento;
import com.GoTicket.Catalogo.Evento.Model.SectorEvento;
import com.GoTicket.Catalogo.Evento.Model.SectorRecinto;
import com.GoTicket.Catalogo.Evento.Model.SectorEventoRequest;
import com.GoTicket.Catalogo.Evento.Repository.EventoRepository;
import com.GoTicket.Catalogo.Evento.Repository.SectorEventoRepository;
import com.GoTicket.Catalogo.Evento.Repository.SectorRecintoRepository;

@ExtendWith(MockitoExtension.class)
class SectorEventoServiceTest {
    @Mock private EventoRepository eventoRepository;
    @Mock private SectorEventoRepository sectorRepository;
    @Mock private SectorRecintoRepository sectorRecintoRepository;

    private SectorEventoService service;

    @BeforeEach
    void setUp() {
        service = new SectorEventoService(eventoRepository, sectorRepository, sectorRecintoRepository);
    }

    @Test
    void guardaSectoresCuandoLaCapacidadCoincideConElEvento() {
        Evento event = new Evento();
        event.setIdEvento(5L);
        event.setCapacidad(100);
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(event));
        when(sectorRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<?> sectors = service.configurar(5L, List.of(
                new SectorEventoRequest("Platea", 60, BigDecimal.valueOf(12000), "GENERAL", "#702bd8", 30, 35),
                new SectorEventoRequest("Cancha", 40, BigDecimal.valueOf(18000), "NUMERADO", "#ff9418", 70, 65)));

        assertEquals(2, sectors.size());
        verify(sectorRepository).deleteByIdEvento(5L);
        verify(sectorRepository).saveAll(anyList());
    }

    @Test
    void rechazaCapacidadQueNoCoincideConElEvento() {
        Evento event = new Evento();
        event.setIdEvento(5L);
        event.setCapacidad(100);
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(event));

        assertThrows(IllegalArgumentException.class, () -> service.configurar(5L, List.of(
                new SectorEventoRequest("Platea", 80, BigDecimal.valueOf(12000), "GENERAL", "#702bd8", 50, 50))));
    }

    @Test
    void rechazaNombresDuplicadosSinImportarMayusculas() {
        Evento event = new Evento();
        event.setIdEvento(5L);
        event.setCapacidad(100);
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(event));

        assertThrows(IllegalArgumentException.class, () -> service.configurar(5L, List.of(
                new SectorEventoRequest("General", 50, BigDecimal.TEN, "GENERAL", "#702bd8", 30, 35),
                new SectorEventoRequest("general", 50, BigDecimal.TEN, "GENERAL", "#ff9418", 70, 65))));
    }

    @Test
    void creaSectorGeneralConPrecioYCapacidadDelEventoCuandoNoHaySectores() {
        Evento event = new Evento();
        event.setIdEvento(5L);
        event.setCapacidad(100);
        event.setPrecio(BigDecimal.valueOf(12000));
        when(sectorRepository.findByIdEventoOrderByIdSector(5L)).thenReturn(List.of());
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(event));

        service.crearSectorGeneralSiFaltan(5L);

        verify(sectorRepository).save(any());
    }

    @Test
    void conservaSectoresQueYaEstanConfigurados() {
        when(sectorRepository.findByIdEventoOrderByIdSector(5L))
                .thenReturn(List.of(new com.GoTicket.Catalogo.Evento.Model.SectorEvento()));

        service.crearSectorGeneralSiFaltan(5L);

        verify(sectorRepository, never()).save(any());
        verify(eventoRepository, never()).findById(5L);
    }

    @Test
    void creaSectoresDelRecintoEscaladosAlAforoYConservaPrecioDelEvento() {
        Evento event = new Evento();
        event.setIdEvento(5L);
        event.setIdRecinto(7L);
        event.setCapacidad(1000);
        event.setPrecio(BigDecimal.valueOf(20000));
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(event));
        when(sectorRepository.findByIdEventoOrderByIdSector(5L)).thenReturn(List.of(
                new SectorEvento(null, 5L, "General", 1000, BigDecimal.valueOf(20000),
                        "GENERAL", "#702bd8", 50, 35)));
        when(sectorRecintoRepository.findByIdRecintoOrderByIdSectorRecinto(7L)).thenReturn(List.of(
                new SectorRecinto(1L, 7L, "Cancha", 600, "GENERAL", "#f97316", 50, 72),
                new SectorRecinto(2L, 7L, "Platea", 400, "NUMERADO", "#2563eb", 50, 40)));
        when(sectorRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        service.configurarSectoresInicialesDesdeRecinto(5L, 7L);

        org.mockito.ArgumentCaptor<List<SectorEvento>> captor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(sectorRepository).saveAll(captor.capture());
        List<SectorEvento> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertEquals("Cancha", saved.get(0).getNombre());
        assertEquals(600, saved.get(0).getCapacidad());
        assertEquals("#f97316", saved.get(0).getColor());
        assertEquals(BigDecimal.valueOf(20000), saved.get(0).getPrecio());
        assertEquals("Platea", saved.get(1).getNombre());
        assertEquals(400, saved.get(1).getCapacidad());
        verify(sectorRepository).deleteByIdEvento(5L);
    }

    @Test
    void noReemplazaSectoresPersonalizados() {
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(new Evento()));
        when(sectorRepository.findByIdEventoOrderByIdSector(5L))
                .thenReturn(List.of(new SectorEvento(null, 5L, "VIP", 50, BigDecimal.TEN,
                        "NUMERADO", "#d97706", 50, 20)));

        service.configurarSectoresInicialesDesdeRecinto(5L, 7L);

        verify(sectorRepository, never()).deleteByIdEvento(5L);
        verify(sectorRepository, never()).saveAll(anyList());
    }
}
