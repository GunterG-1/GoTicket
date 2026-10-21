package com.GoTicket.Pagos.Messaging;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
@RequiredArgsConstructor
public class SqsMessagePoller {
    private static final Logger log = LoggerFactory.getLogger(SqsMessagePoller.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final OrderCreatedListener orderListener;
    private final PaymentExpirationListener expirationListener;

    @Value("${goticket.aws.queue.order-created-url}")
    private String orderQueueUrl;
    @Value("${goticket.aws.queue.payment-failed-url}")
    private String failedQueueUrl;

    @Scheduled(fixedDelayString = "${goticket.aws.poll-ms:1000}")
    public void poll() {
        receive(orderQueueUrl, true);
        receive(failedQueueUrl, false);
    }

    private void receive(String queueUrl, boolean orderCreated) {
        if (queueUrl == null || queueUrl.isBlank()) return;
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl).maxNumberOfMessages(10).waitTimeSeconds(1).visibilityTimeout(30).build()).messages();
        for (Message message : messages) {
            try {
                JsonNode payload = messagePayload(message);
                if (orderCreated) {
                    orderListener.handle(objectMapper.treeToValue(payload, OrderCreatedEvent.class));
                } else {
                    expirationListener.handle(objectMapper.treeToValue(payload, PaymentStatusEvent.class));
                }
                sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle()).build());
            } catch (Exception exception) {
                log.warn("No se pudo procesar el mensaje de SQS de Pagos; se reintentará", exception);
            }
        }
    }

    private JsonNode messagePayload(Message message) throws Exception {
        JsonNode envelope = objectMapper.readTree(message.body());
        return envelope.has("Message") ? objectMapper.readTree(envelope.get("Message").asText()) : envelope;
    }
}
