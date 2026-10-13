package com.lagosrent.payment.infrastructure.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {
    // 1. Exchanges
    public static final String DOMAIN_EXCHANGE = "platform.domain.v1";
    public static final String DEAD_LETTER_EXCHANGE = "platform.dlx.v1";

    // 2. Routing Keys
    public static final String ROUTING_KEY_PAYMENT_SUCCEEDED = "payment.succeeded.v1";
    public static final String ROUTING_KEY_PAYMENT_FAILED = "payment.failed.v1";

    // 3 Queue Declaration
    public static final String QUEUE_PAYMENT_EVENTS = "payment.events.v1";
    public static final String QUEUE_PAYMENT_EVENTS_DLQ = "payment.events.dlq";

    @Bean
    public TopicExchange domainExchange() {
        return ExchangeBuilder.topicExchange(DOMAIN_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return ExchangeBuilder.topicExchange(DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue paymentEventsQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", QUEUE_PAYMENT_EVENTS_DLQ);
        return QueueBuilder.durable(QUEUE_PAYMENT_EVENTS)
                .withArguments(args)
                .build();
    }

    @Bean
    public Queue paymentEventsDlq() {
        return QueueBuilder.durable(QUEUE_PAYMENT_EVENTS_DLQ).build();
    }

    // Binding to receive rental.reservation-confirmed.v1
    @Bean
    public Binding bindRentalReservationConfirmed(Queue paymentEventsQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(paymentEventsQueue)
                .to(domainExchange)
                .with("rental.reservation-confirmed.v1");
    }

    @Bean
    public Binding bindPaymentDlq(Queue paymentEventsDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(paymentEventsDlq)
                .to(deadLetterExchange)
                .with(QUEUE_PAYMENT_EVENTS_DLQ);
    }

    // JSON Message Converter
    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setTrustedPackages("*");
        typeMapper.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory factory =
                new org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new org.springframework.amqp.support.converter.SimpleMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        return template;
    }
}
