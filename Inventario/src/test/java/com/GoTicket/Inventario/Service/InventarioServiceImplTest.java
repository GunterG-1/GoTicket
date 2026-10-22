package com.GoTicket.Inventario.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.SnsClient;

import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EntradaReservaRequest;
import com.GoTicket.Inventario.Model.EstadoEntrada;
import com.GoTicket.Inventario.Model.Inventario;
import com.GoTicket.Inventario.Model.ConfirmarReservaRequest;
import com.GoTicket.Inventario.Model.CancelarReservaRequest;
import com.GoTicket.Inventario.Model.BloqueoInventario;
import com.GoTicket.Inventario.Model.TransferenciaEntrada;
import com.GoTicket.Inventario.Model.PublicarReventaRequest;
import com.GoTicket.Inventario.Model.TransferirEntradaRequest;
import com.GoTicket.Inventario.Client.CatalogoEvento;
import com.GoTicket.Inventario.Client.CatalogoEventoClient;
import com.GoTicket.Inventario.Client.SectorEvento;
import com.GoTicket.Inventario.Repository.EntradaRepository;
import com.GoTicket.Inventario.Repository.InventarioRepository;
import com.GoTicket.Inventario.Repository.BloqueoInventarioRepository;
import com.GoTicket.Inventario.Repository.TransferenciaEntradaRepository;

@ExtendWith(MockitoExtension.class)
class InventarioServiceImplTest {
    @Mock private InventarioRepository inventarioRepository;
    @Mock private EntradaRepository entradaRepository;
    @Mock private TransferenciaEntradaRepository transferenciaRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private CatalogoEventoClient catalogoEventoClient;
    @Mock private BloqueoInventarioRepository bloqueoRepository;
    @Mock private SnsClient snsClient;
    @Mock private ObjectMapper objectMapper;

    private InventarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InventarioServiceImpl(inventarioRepository, entradaRepository, redisTemplate, transferenciaRepository,
            catalogoEventoClient, bloqueoRepository, snsClient, objectMapper);
    }

    @Test
    void rechazaReservaSinDatosObligatorios() {
        assertThrows(IllegalArgumentException.class, () -> service.reservarEntradas(new EntradaReservaRequest(null, 7L, "General", 1, "123e4567-e89b-12d3-a456-426614174000")));
    }

    @Test
    void guardarInventarioEsIdempotenteYConservaElExistente() {
        Inventario existing = new Inventario();
        existing.setIdEvento(49L);
        existing.setCantidadTotal(100);
        existing.setCantidadDisponible(87);
        when(inventarioRepository.findByIdEvento(49L)).thenReturn(Optional.of(existing));

        Inventario requested = new Inventario();
        requested.setIdEvento(49L);
        requested.setCantidadTotal(100);
        requested.setCantidadDisponible(100);

        Inventario result = service.guardarInventario(requested);

        assertEquals(existing, result);
        assertEquals(87, result.getCantidadDisponible());
        verify(inventarioRepository).crearSiNoExiste(
                org.mockito.ArgumentMatchers.eq(49L),
                org.mockito.ArgumentMatchers.eq(100),
                org.mockito.ArgumentMatchers.eq(100),
                org.mockito.ArgumentMatchers.eq(true),
                any(LocalDateTime.class));
    }

    @Test
    void rechazaReservaCuandoNoHayDisponibilidad() {
        Inventario inventory = new Inventario();
        inventory.setIdEvento(10L);
        inventory.setCantidadDisponible(0);
        inventory.setCantidadTotal(10);
        when(catalogoEventoClient.buscarEvento(10L)).thenReturn(Optional.of(eventoActivo(10)));
        when(catalogoEventoClient.listarSectores(10L)).thenReturn(List.of(
                new SectorEvento(1L, 10L, "General", 10, BigDecimal.TEN, "GENERAL", "#702bd8", 50, 35)));
        when(inventarioRepository.findByIdEventoForUpdate(10L)).thenReturn(Optional.of(inventory));

        assertThrows(IllegalStateException.class, () -> service.reservarEntradas(new EntradaReservaRequest(10L, 7L, "General", 1, "123e4567-e89b-12d3-a456-426614174000")));
    }

    @Test
    void reservaEntradasDuranteCincoMinutos() {
        Entrada entry = new Entrada(1L, 10L, "General", "1", BigDecimal.TEN,
                BigDecimal.TEN, EstadoEntrada.DISPONIBLE, null, null, null, null, null, null, null, null);
        Inventario inventory = new Inventario();
        inventory.setIdEvento(10L);
        inventory.setCantidadDisponible(10);
        inventory.setCantidadTotal(10);
        when(catalogoEventoClient.buscarEvento(10L)).thenReturn(Optional.of(eventoActivo(10)));
        when(catalogoEventoClient.listarSectores(10L)).thenReturn(List.of(
                new SectorEvento(1L, 10L, "General", 10, BigDecimal.TEN, "GENERAL", "#702bd8", 50, 35)));
        when(inventarioRepository.findByIdEventoForUpdate(10L)).thenReturn(Optional.of(inventory));
        when(entradaRepository.findByIdEvento(10L)).thenReturn(List.of());
        when(entradaRepository.findByIdEventoAndSectorAndEstado(10L, "General", EstadoEntrada.DISPONIBLE)).thenReturn(List.of(entry));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(true);
        when(entradaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        String reservationId = "123e4567-e89b-12d3-a456-426614174000";
        List<Entrada> result = service.reservarEntradas(new EntradaReservaRequest(10L, 7L, "General", 1, reservationId));

        assertEquals(EstadoEntrada.BLOQUEADO, result.get(0).getEstado());
        assertEquals(7L, result.get(0).getIdUsuario());
        assertEquals(reservationId, result.get(0).getIdReserva());
        verify(valueOperations).setIfAbsent(any(), any(), org.mockito.ArgumentMatchers.eq(java.time.Duration.ofMinutes(5)));
    }

    @Test
    void calculaDisponibilidadDistribuidaPorSector() {
        Inventario inventory = new Inventario();
        inventory.setIdEvento(10L);
        inventory.setCantidadDisponible(10);
        inventory.setCantidadTotal(10);
        when(inventarioRepository.findByIdEvento(10L)).thenReturn(Optional.of(inventory));
        when(entradaRepository.findByIdEvento(10L)).thenReturn(List.of());
        when(catalogoEventoClient.listarSectores(10L)).thenReturn(List.of(
                new SectorEvento(1L, 10L, "General", 10, BigDecimal.TEN,
                        "GENERAL", "#702bd8", 50, 35)));

        List<com.GoTicket.Inventario.Model.SectorDisponibilidad> result = service.disponibilidadPorSector(10L);

        assertEquals(10, result.stream().mapToInt(com.GoTicket.Inventario.Model.SectorDisponibilidad::cantidadTotal).sum());
        assertEquals(10, result.stream().mapToInt(com.GoTicket.Inventario.Model.SectorDisponibilidad::cantidadDisponible).sum());
        assertEquals("General", result.get(0).sector());
        assertEquals(BigDecimal.TEN, result.get(0).precio());
        assertEquals(50, result.get(0).posicionX());
        assertEquals(35, result.get(0).posicionY());
    }

    @Test
    void confirmarReservaDescuentaDisponibilidadUnaSolaVez() {
        Entrada entry = new Entrada(1L, 10L, "General", "1", BigDecimal.TEN,
                BigDecimal.TEN, EstadoEntrada.BLOQUEADO, 7L, null,
                "123e4567-e89b-12d3-a456-426614174000", null,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(5),
                null, null);
        Inventario inventory = new Inventario();
        inventory.setIdEvento(10L);
        inventory.setCantidadDisponible(10);
        inventory.setCantidadTotal(10);
        when(entradaRepository.findByIdEventoAndIdUsuarioAndIdReservaAndEstado(
                10L, 7L, "123e4567-e89b-12d3-a456-426614174000", EstadoEntrada.BLOQUEADO))
                .thenReturn(List.of(entry));
        when(entradaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventarioRepository.findByIdEventoForUpdate(10L)).thenReturn(Optional.of(inventory));
        when(bloqueoRepository.findByIdOrden(99L)).thenReturn(Optional.empty());
        when(bloqueoRepository.save(any(BloqueoInventario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.confirmarReserva(new ConfirmarReservaRequest(10L, 7L, 99L, "123e4567-e89b-12d3-a456-426614174000"));

        assertEquals(9, inventory.getCantidadDisponible());
        assertEquals(99L, entry.getIdOrden());
        verify(inventarioRepository).save(inventory);
        verify(bloqueoRepository).save(any(BloqueoInventario.class));
    }

    @Test
    void cancelarCheckoutLiberalaReservaYRestituyeElStockConfirmado() {
        String reservationId = "123e4567-e89b-12d3-a456-426614174000";
        Entrada entry = new Entrada(1L, 10L, "General", "1", BigDecimal.TEN,
                BigDecimal.TEN, EstadoEntrada.BLOQUEADO, 7L, 99L,
                reservationId, null, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5), null, null);
        Inventario inventory = new Inventario();
        inventory.setIdEvento(10L);
        inventory.setCantidadDisponible(8);
        inventory.setCantidadTotal(10);
        BloqueoInventario lock = new BloqueoInventario(
                1L, 99L, 10L, 2, LocalDateTime.now().plusMinutes(5), false);
        when(entradaRepository.findByIdReservaAndEstadoForUpdate(reservationId, EstadoEntrada.BLOQUEADO))
                .thenReturn(List.of(entry));
        when(bloqueoRepository.findByIdOrdenForUpdate(99L)).thenReturn(Optional.of(lock));
        when(inventarioRepository.findByIdEventoForUpdate(10L)).thenReturn(Optional.of(inventory));
        when(entradaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelarReserva(new CancelarReservaRequest(reservationId, 7L, 99L));

        assertEquals(EstadoEntrada.DISPONIBLE, entry.getEstado());
        assertEquals(null, entry.getIdReserva());
        assertEquals(10, inventory.getCantidadDisponible());
        org.junit.jupiter.api.Assertions.assertTrue(lock.isResuelto());
        verify(inventarioRepository).save(inventory);
    }

    @Test
    void rechazaPrecioDeReventaSuperiorAl95PorCientoDelOriginal() {
        Entrada entry = new Entrada();
        entry.setIdEntrada(12L);
        entry.setIdEvento(10L);
        entry.setIdUsuario(7L);
        entry.setEstado(EstadoEntrada.VENDIDO);
        entry.setPrecio(BigDecimal.TEN);
        entry.setPrecioOriginal(BigDecimal.TEN);
        when(entradaRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(entry));

        assertThrows(IllegalArgumentException.class,
                () -> service.publicarReventa(12L, new PublicarReventaRequest(7L, BigDecimal.valueOf(9.51))));
    }

    @Test
    void permitePublicarAl95PorCientoDelPrecioOriginal() {
        Entrada entry = new Entrada();
        entry.setIdEntrada(12L);
        entry.setIdEvento(10L);
        entry.setIdUsuario(7L);
        entry.setEstado(EstadoEntrada.VENDIDO);
        entry.setPrecio(BigDecimal.TEN);
        entry.setPrecioOriginal(BigDecimal.TEN);
        when(entradaRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(entry));
        when(catalogoEventoClient.buscarEvento(10L)).thenReturn(Optional.of(eventoActivo(100)));
        when(entradaRepository.save(any(Entrada.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Entrada listed = service.publicarReventa(12L, new PublicarReventaRequest(7L, BigDecimal.valueOf(9.50)));

        assertEquals(EstadoEntrada.EN_REVENTA, listed.getEstado());
        assertEquals(BigDecimal.valueOf(9.50), listed.getPrecio());
    }

    @Test
    void cancelarCheckoutDeReventaLiberaLaEntradaAlCompradorCorrecto() {
        Entrada entry = new Entrada();
        entry.setIdEntrada(12L);
        entry.setEstado(EstadoEntrada.EN_REVENTA);
        entry.setIdOrden(99L);
        entry.setIdUsuarioReserva(7L);
        entry.setExpiraEn(LocalDateTime.now().plusMinutes(4));
        when(entradaRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(entry));
        when(entradaRepository.save(any(Entrada.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelarReservaReventa(12L, new TransferirEntradaRequest(7L, 99L));

        assertEquals(null, entry.getIdOrden());
        assertEquals(null, entry.getIdUsuarioReserva());
        assertEquals(null, entry.getExpiraEn());
    }

    @Test
    void marcaUnaEntradaComoUsadaAlValidarSuQr() {
        Entrada entry = new Entrada();
        entry.setIdEntrada(12L);
        entry.setCodigoAcceso("unique-ticket-code");
        entry.setEstado(EstadoEntrada.VENDIDO);
        when(entradaRepository.findByCodigoAccesoForUpdate("unique-ticket-code")).thenReturn(Optional.of(entry));
        when(entradaRepository.save(any(Entrada.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Entrada used = service.validarAcceso("unique-ticket-code");

        assertEquals(EstadoEntrada.USADA, used.getEstado());
        assertEquals(null, used.getCodigoAcceso());
        org.junit.jupiter.api.Assertions.assertNotNull(used.getFechaUso());
    }

    @Test
    void noPermiteReutilizarUnQrYaValidado() {
        Entrada entry = new Entrada();
        entry.setEstado(EstadoEntrada.USADA);
        when(entradaRepository.findByCodigoAccesoForUpdate("used-code")).thenReturn(Optional.of(entry));

        assertThrows(IllegalStateException.class, () -> service.validarAcceso("used-code"));
    }

    private CatalogoEvento eventoActivo(int capacity) {
        return new CatalogoEvento(10L, capacity, BigDecimal.TEN, "Evento", "musica", "Otro lugar",
                LocalDateTime.now().plusDays(1), true);
    }
}
