package com.lagosrent.notification.infrastructure.messaging.config;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static java.time.Duration.ofMillis;

@Slf4j
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
        // Strict manual acknowledgement
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    // In-Consumer Bounded Retry Template
    @Bean
    public RetryTemplate consumerRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(ofMillis(1000), 2.0, ofMillis(8000))
                .retryOn(List.of(
                        ResourceAccessException.class,
                        ConnectException.class,
                        SocketTimeoutException.class,
                        TimeoutException.class, //  Handles Resilience4j TimeLimiter timeout!
                        CallNotPermittedException.class
                ))
                .traversingCauses()
                .withListener(new RetryListener() {
                    @Override
                    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                        log.warn("Transient failure in event listener (attempt #{}/3. Root cause: [{}] {}",
                                context.getRetryCount(),
                                throwable.getClass().getName(),
                                throwable.getMessage(),
                                throwable
                        );
                    }
                })
                .build();
    }
}
