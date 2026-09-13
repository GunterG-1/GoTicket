package com.GoTicket.Orden.Config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Value("${goticket.rabbit.exchange}")
    private String exchangeName;
    @Value("${goticket.rabbit.payment-approved-queue}")
    private String approvedQueueName;
    @Value("${goticket.rabbit.payment-failed-queue}")
    private String failedQueueName;
    @Value("${goticket.rabbit.payment-approved-key}")
    private String approvedKey;
    @Value("${goticket.rabbit.payment-failed-key}")
    private String failedKey;

    @Bean
    DirectExchange goticketExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    Queue paymentApprovedQueue() { return new Queue(approvedQueueName, true); }

    @Bean
    Queue paymentFailedQueue() { return new Queue(failedQueueName, true); }

    @Bean
    org.springframework.amqp.core.Binding paymentApprovedBinding() {
        return org.springframework.amqp.core.BindingBuilder.bind(paymentApprovedQueue()).to(goticketExchange()).with(approvedKey);
    }

    @Bean
    org.springframework.amqp.core.Binding paymentFailedBinding() {
        return org.springframework.amqp.core.BindingBuilder.bind(paymentFailedQueue()).to(goticketExchange()).with(failedKey);
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}