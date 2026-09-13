package com.GoTicket.Orden.Service;

import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.GoTicket.Orden.Messaging.OrderCreatedEvent;
import com.GoTicket.Orden.Model.OutboxEvent;
import com.GoTicket.Orden.Repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxPublisher {
    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${goticket.rabbit.exchange}")
    private String exchangeName;
    @Value("${goticket.rabbit.order-routing-key}")
    private String routingKey;

    @Scheduled(fixedDelayString = "${goticket.outbox.poll-ms:2000}")
    @Transactional
    public void publishPending() {
        repository.findTop50ByPublishedFalseOrderByCreatedAtAsc().forEach(this::publish);
    }

    private void publish(OutboxEvent event) {
        try {
            OrderCreatedEvent payload = objectMapper.readValue(event.getPayload(), OrderCreatedEvent.class);
            rabbitTemplate.convertAndSend(exchangeName, routingKey, payload);
            event.setPublished(true);
            event.setPublishedAt(LocalDateTime.now());
        } catch (Exception exception) {
            event.setAttempts(event.getAttempts() + 1);
        }
        repository.save(event);
    }
}