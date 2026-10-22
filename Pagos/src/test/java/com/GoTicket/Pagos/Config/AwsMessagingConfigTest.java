package com.GoTicket.Pagos.Config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.GoTicket.Pagos.Messaging.OrderCreatedEvent;

class AwsMessagingConfigTest {

    @Test
    void objectMapperDeserializaLocalDateTimeDeOrderCreatedEvent() throws Exception {
        var mapper = new AwsMessagingConfig().objectMapper();
        LocalDateTime expiration = LocalDateTime.of(2026, 11, 1, 18, 30);
        String message = """
                {
                  "idOrden": 9,
                  "idEvento": 10,
                  "idUsuario": 11,
                  "cantidad": 2,
                  "total": 15000.00,
                  "sector": "General",
                  "idEntradaReventa": null,
                  "idReserva": "reserva-1",
                  "expiraReserva": "2026-11-01T18:30:00"
                }
                """;

        OrderCreatedEvent event = mapper.readValue(message, OrderCreatedEvent.class);

        assertEquals(expiration, event.expiraReserva());
        assertNotNull(mapper.writeValueAsString(event));
    }
}
