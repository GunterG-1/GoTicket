package com.GoTicket.Pagos.Config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class RabbitConfig {
    @Value("${goticket.rabbit.exchange}")
    private String exchangeName;
    @Value("${goticket.rabbit.order-queue}")
    private String queueName;
    @Value("${goticket.rabbit.order-routing-key}")
    private String routingKey;
    @Value("${goticket.rabbit.payment-approved-key}")
    private String approvedKey;
    @Value("${goticket.rabbit.payment-failed-key}")
    private String failedKey;
    @Value("${goticket.rabbit.payment-failed-queue}")
    private String failedQueueName;

    @Bean
    DirectExchange goticketExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    Queue orderCreatedQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    Binding orderCreatedBinding(Queue orderCreatedQueue, DirectExchange goticketExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(goticketExchange).with(routingKey);
    }

    @Bean
    Queue paymentFailedQueue() {
        return new Queue(failedQueueName, true);
    }

    @Bean
    Binding paymentFailedBinding(Queue paymentFailedQueue, DirectExchange goticketExchange) {
        return BindingBuilder.bind(paymentFailedQueue).to(goticketExchange).with(failedKey);
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