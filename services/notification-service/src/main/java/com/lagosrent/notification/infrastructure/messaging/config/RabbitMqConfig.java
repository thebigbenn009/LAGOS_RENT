package com.lagosrent.notification.infrastructure.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {
    public static final String DOMAIN_EXCHANGE = "platform.domain.v1";
    public static final String DEAD_LETTER_EXCHANGE = "platform.dlx.v1";

    public static final String QUEUE_NOTIFICATION_EVENTS = "notification.events.v1";
    public static final String QUEUE_NOTIFICATION_EVENTS_DLQ = "notification.events.dlq";

    @Bean
    public TopicExchange domainExchange() {
        return ExchangeBuilder.topicExchange(DOMAIN_EXCHANGE).durable(true).build();
    }

    @Bean
    TopicExchange deadLetterExchange() {
        return ExchangeBuilder.topicExchange(DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue notificationEventsQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", QUEUE_NOTIFICATION_EVENTS_DLQ);
        return QueueBuilder.durable(QUEUE_NOTIFICATION_EVENTS)
                .withArguments(args)
                .build();
    }

    @Bean
    public Queue notificationEventsDlq() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_EVENTS_DLQ).build();
    }

    // --- Bindings: Connecting Exchange to Notification Queue ---

    @Bean
    public Binding bindNotificationPayments(Queue notificationEventsQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(domainExchange).with("payment.#");
    }

    @Bean
    public Binding bindNotificationTenancy(Queue notificationEventsQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(domainExchange).with("tenancy.#");
    }

    @Bean
    public Binding bindNotificationRentals(Queue notificationEventsQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(domainExchange).with("rental.reservation-confirmed.v1");
    }

    @Bean
    Binding bindNotificationLandlords(Queue notificationEventsQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(domainExchange).with("landlord.verified.v1");
    }

    @Bean
    Binding bindNotificationDlq(Queue notificationEventsDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(notificationEventsDlq).to(deadLetterExchange).with(QUEUE_NOTIFICATION_EVENTS_DLQ);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new SimpleMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }
}
