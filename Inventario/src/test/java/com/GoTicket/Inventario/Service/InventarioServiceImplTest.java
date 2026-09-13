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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.GoTicket.Inventario.Model.Entrada;
import com.GoTicket.Inventario.Model.EntradaReservaRequest;
import com.GoTicket.Inventario.Model.EstadoEntrada;
import com.GoTicket.Inventario.Model.Inventario;
import com.GoTicket.Inventario.Model.TransferenciaEntrada;
import com.GoTicket.Inventario.Client.CatalogoEvento;
import com.GoTicket.Inventario.Client.CatalogoEventoClient;
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
    @Mock private RabbitTemplate rabbitTemplate;

    private InventarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InventarioServiceImpl(inventarioRepository, entradaRepository, redisTemplate, transferenciaRepository,
            catalogoEventoClient, bloqueoRepository, rabbitTemplate);
    }

    @Test
    void rechazaReservaSinDatosObligatorios() {
        assertThrows(IllegalArgumentException.class, () -> service.reservarEntradas(new EntradaReservaRequest(null, 7L, "General", 1)));
    }

    @Test
    void rechazaReservaCuandoNoHayDisponibilidad() {
        Inventario inventory = new Inventario();
        inventory.setIdEvento(10L);
        inventory.setCantidadDisponible(0);
        inventory.setCantidadTotal(10);
        when(catalogoEventoClient.buscarEvento(10L)).thenReturn(Optional.of(new CatalogoEvento(10L, 10, BigDecimal.TEN)));
        when(inventarioRepository.findByIdEvento(10L)).thenReturn(Optional.of(inventory));

        assertThrows(IllegalStateException.class, () -> service.reservarEntradas(new EntradaReservaRequest(10L, 7L, "General", 1)));
    }

    @Test
    void reservaEntradasDuranteCincoMinutos() {
        Entrada entry = new Entrada(1L, 10L, "General", "1", BigDecimal.TEN, EstadoEntrada.DISPONIBLE, null, null, null, null);
        Inventario inventory = new Inventario();
        inventory.setIdEvento(10L);
        inventory.setCantidadDisponible(10);
        inventory.setCantidadTotal(10);
        when(catalogoEventoClient.buscarEvento(10L)).thenReturn(Optional.of(new CatalogoEvento(10L, 10, BigDecimal.TEN)));
        when(inventarioRepository.findByIdEvento(10L)).thenReturn(Optional.of(inventory));
        when(entradaRepository.findByIdEventoAndSectorAndEstado(10L, "General", EstadoEntrada.DISPONIBLE)).thenReturn(List.of(entry));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(true);
        when(entradaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Entrada> result = service.reservarEntradas(new EntradaReservaRequest(10L, 7L, "General", 1));

        assertEquals(EstadoEntrada.BLOQUEADO, result.get(0).getEstado());
        assertEquals(7L, result.get(0).getIdUsuario());
        verify(valueOperations).setIfAbsent(any(), any(), org.mockito.ArgumentMatchers.eq(java.time.Duration.ofMinutes(5)));
    }
}
