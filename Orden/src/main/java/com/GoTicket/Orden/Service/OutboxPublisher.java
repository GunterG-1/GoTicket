package com.GoTicket.Orden.Service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.GoTicket.Orden.Messaging.OrderCreatedEvent;
import com.GoTicket.Orden.Model.OutboxEvent;
import com.GoTicket.Orden.Repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxPublisher {
    private final OutboxEventRepository repository;
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Value("${goticket.aws.sns.topic-arn}")
    private String topicArn;

    @Scheduled(fixedDelayString = "${goticket.outbox.poll-ms:2000}")
    @Transactional
    public void publishPending() {
        repository.findTop50ByPublishedFalseOrderByCreatedAtAsc().forEach(this::publish);
    }

    private void publish(OutboxEvent event) {
        try {
            OrderCreatedEvent payload = objectMapper.readValue(event.getPayload(), OrderCreatedEvent.class);
            snsClient.publish(PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(objectMapper.writeValueAsString(payload))
                    .messageAttributes(java.util.Map.of(
                            "eventType", software.amazon.awssdk.services.sns.model.MessageAttributeValue.builder()
                                    .dataType("String").stringValue("orden.creada").build()))
                    .build());
            event.setPublished(true);
            event.setPublishedAt(LocalDateTime.now());
        } catch (Exception exception) {
            event.setAttempts(event.getAttempts() + 1);
        }
        repository.save(event);
    }
}
