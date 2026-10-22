package com.GoTicket.Inventario.Config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.GoTicket.Inventario.Messaging.OrderCreatedEvent;

class AwsMessagingConfigTest {
    @Test
    void deserializaFechaDeExpiracionDeReservaDesdeElEventoSqs() throws Exception {
        var mapper = new AwsMessagingConfig().objectMapper();
        String payload = """
                {
                  "idOrden": 8,
                  "idEvento": 34,
                  "idUsuario": 672719294,
                  "cantidad": 1,
                  "total": 30000.00,
                  "sector": "Preferencial",
                  "idEntradaReventa": null,
                  "idReserva": "b075b723-0f5c-4a3c-afaf-3419982f156c",
                  "expiraReserva": "2026-10-22T06:13:50.227"
                }
                """;

        OrderCreatedEvent event = mapper.readValue(payload, OrderCreatedEvent.class);

        assertEquals(LocalDateTime.of(2026, 10, 22, 6, 13, 50, 227_000_000),
                event.expiraReserva());
    }
}
