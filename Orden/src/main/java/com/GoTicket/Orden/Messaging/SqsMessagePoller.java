package com.GoTicket.Orden.Messaging;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final PaymentStatusListener listener;

    @Value("${goticket.aws.queue.payment-approved-url}")
    private String approvedQueueUrl;
    @Value("${goticket.aws.queue.payment-failed-url}")
    private String failedQueueUrl;

    @Scheduled(fixedDelayString = "${goticket.aws.poll-ms:1000}")
    public void poll() {
        receive(approvedQueueUrl, "pago.aprobado");
        receive(failedQueueUrl, "pago.fallido");
    }

    private void receive(String queueUrl, String eventType) {
        if (queueUrl == null || queueUrl.isBlank()) return;
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl).maxNumberOfMessages(10).waitTimeSeconds(1).visibilityTimeout(30).build()).messages();
        for (Message message : messages) {
            try {
                PaymentStatusEvent event = objectMapper.treeToValue(messagePayload(message), PaymentStatusEvent.class);
                if ("pago.aprobado".equals(eventType)) listener.approved(event);
                else listener.failed(event);
                sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle()).build());
            } catch (Exception ignored) {
                // The message remains visible for SQS redelivery.
            }
        }
    }

    private JsonNode messagePayload(Message message) throws Exception {
        JsonNode envelope = objectMapper.readTree(message.body());
        return envelope.has("Message") ? objectMapper.readTree(envelope.get("Message").asText()) : envelope;
    }
}
