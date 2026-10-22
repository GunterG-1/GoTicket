package com.GoTicket.Catalogo.Evento.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import com.GoTicket.Catalogo.Evento.Model.Recinto;
import com.GoTicket.Catalogo.Evento.Model.SectorRecintoRequest;
import com.GoTicket.Catalogo.Evento.Repository.RecintoRepository;
import com.GoTicket.Catalogo.Evento.Repository.SectorRecintoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SectorRecintoServiceTest {
    @Mock private RecintoRepository recintoRepository;
    @Mock private SectorRecintoRepository sectorRepository;

    private SectorRecintoService service;

    @BeforeEach
    void setUp() {
        service = new SectorRecintoService(recintoRepository, sectorRepository);
    }

    @Test
    void guardaMapaCuandoLasCapacidadesCoincidenConElRecinto() {
        when(recintoRepository.findById(7L))
                .thenReturn(Optional.of(new Recinto(7L, "Arena", "Santiago", "Dirección", 100)));
        when(sectorRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<?> saved = service.configurar(7L, List.of(
                new SectorRecintoRequest("Platea", 60, "GENERAL", "#702bd8", 30, 35),
                new SectorRecintoRequest("Cancha", 40, "NUMERADO", "#ff9418", 70, 65)));

        assertEquals(2, saved.size());
        verify(sectorRepository).deleteByIdRecinto(7L);
        verify(sectorRepository).saveAll(anyList());
    }

    @Test
    void rechazaMapaConCapacidadDiferenteAlAforo() {
        when(recintoRepository.findById(7L))
                .thenReturn(Optional.of(new Recinto(7L, "Arena", "Santiago", "Dirección", 100)));

        assertThrows(IllegalArgumentException.class, () -> service.configurar(7L, List.of(
                new SectorRecintoRequest("Platea", 80, "GENERAL", "#702bd8", 50, 50))));
    }

    @Test
    void creaMapaDeEstadioConSectoresColoreadosYCapacidadCompleta() {
        when(sectorRepository.findByIdRecintoOrderByIdSectorRecinto(7L)).thenReturn(List.of());
        when(recintoRepository.findById(7L))
                .thenReturn(Optional.of(new Recinto(7L, "Estadio Monumental", "Macul", "Dirección", 1000)));
        when(sectorRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        service.crearMapaInicialSiFalta(7L);

        org.mockito.ArgumentCaptor<List<com.GoTicket.Catalogo.Evento.Model.SectorRecinto>> captor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(sectorRepository).saveAll(captor.capture());
        List<com.GoTicket.Catalogo.Evento.Model.SectorRecinto> sectors = captor.getValue();
        assertEquals(5, sectors.size());
        assertEquals(1000, sectors.stream().mapToInt(sector -> sector.getCapacidad()).sum());
        assertEquals("Cancha", sectors.get(0).getNombre());
        assertEquals("#f97316", sectors.get(0).getColor());
    }

    @Test
    void actualizaMapaGeneralPredeterminadoPeroConservaMapaPersonalizado() {
        when(recintoRepository.findById(7L))
                .thenReturn(Optional.of(new Recinto(7L, "Teatro Coliseo", "Santiago", "Dirección", 1000)));
        when(sectorRepository.findByIdRecintoOrderByIdSectorRecinto(7L)).thenReturn(List.of(
                new com.GoTicket.Catalogo.Evento.Model.SectorRecinto(
                        1L, 7L, "General", 1000, "GENERAL", "#702bd8", 50, 35)));
        when(sectorRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        service.crearMapaInicialSiFalta(7L);

        verify(sectorRepository).deleteByIdRecinto(7L);
        verify(sectorRepository).saveAll(anyList());

        when(sectorRepository.findByIdRecintoOrderByIdSectorRecinto(8L)).thenReturn(List.of(
                new com.GoTicket.Catalogo.Evento.Model.SectorRecinto(
                        2L, 8L, "Platea", 1000, "NUMERADO", "#2563eb", 50, 50)));
        service.crearMapaInicialSiFalta(8L);
        verify(sectorRepository, never()).deleteByIdRecinto(8L);
    }
}
