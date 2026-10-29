package com.lagosrent.rental.infrastructure.messaging.config;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.*;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
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
import static org.springframework.amqp.core.AcknowledgeMode.MANUAL;

@Configuration
@Slf4j
public class RabbitMqConfig {
    // 1. Exchanges
    public static final String DOMAIN_EXCHANGE = "platform.domain.v1";
    public static final String DEAD_LETTER_EXCHANGE = "platform.dlx.v1";

    // 2. Routing Keys
    public static final String ROUTING_KEY_LISTING_PUBLISHED = "listing.published.v1";
    public static final String ROUTING_KEY_LISTING_UPDATED = "listing.updated.v1";
    public static final String ROUTING_KEY_LANDLORD_VERIFIED = "landlord.verified.v1";
    public static final String ROUTING_KEY_RESERVATION_REQUESTED = "rental.reservation-requested.v1";
    public static final String ROUTING_KEY_LISTING_RESERVED = "listing.reserved.v1";
    public static final String ROUTING_KEY_TENANCY_CONFIRMED = "tenancy.confirmed.v1";

    // 3 Queues
    public static final String QUEUE_SEARCH_LISTINGS = "search.listing-projection.v1";
    public static final String QUEUE_SEARCH_LISTINGS_DLQ = "search.listing-projection.dlq";

    public static final String QUEUE_LISTING_EVENTS = "listing.events.v1";
    public static final String QUEUE_LISTING_EVENTS_DLQ = "listing.events.dlq";

    public static final String QUEUE_NOTIFICATION_EVENTS = "notification.events.v1";
    public static final String QUEUE_NOTIFICATION_EVENTS_DLQ = "notification.events.dlq";

    public static final String QUEUE_RENTAL_WORKFLOW = "rental.workflow.v1";
    public static final String QUEUE_RENTAL_WORKFLOW_DLQ = "rental.workflow.dlq";

    @Bean
    public TopicExchange domainExchange() {
        return ExchangeBuilder.topicExchange(DOMAIN_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return ExchangeBuilder.topicExchange(DEAD_LETTER_EXCHANGE)
                .durable(true)
                .build();
    }

    // Queue Declarations with Dead-Letter routing
    @Bean
    public Queue searchListingsQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", QUEUE_SEARCH_LISTINGS_DLQ);
        return QueueBuilder.durable(QUEUE_SEARCH_LISTINGS)
                .withArguments(args)
                .build();
    }

    @Bean
    public Queue searchListingsDlq() {
        return QueueBuilder.durable(QUEUE_SEARCH_LISTINGS_DLQ).build();
    }

    @Bean
    public Queue listingEventsQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", QUEUE_LISTING_EVENTS_DLQ);
        return QueueBuilder.durable(QUEUE_LISTING_EVENTS)
                .withArguments(args)
                .build();
    }

    @Bean
    public Queue listingEventsDlq() {
        return QueueBuilder.durable(QUEUE_LISTING_EVENTS_DLQ).build();
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

    @Bean
    public Queue rentalWorkflowQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", QUEUE_RENTAL_WORKFLOW_DLQ);
        return QueueBuilder.durable(QUEUE_RENTAL_WORKFLOW)
                .withArguments(args)
                .build();
    }

    @Bean
    public Queue rentalWorkflowDlq() {
        return QueueBuilder.durable(QUEUE_RENTAL_WORKFLOW_DLQ).build();
    }

    // Bindings: Connecting Exchanges to Queues
    @Bean
    public Binding bindSearchListingPublished(Queue searchListingsQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(searchListingsQueue).to(domainExchange).with("listing.*.v1");
    }

    @Bean
    public Binding bindSearchDlq(Queue searchListingsDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(searchListingsDlq).to(deadLetterExchange).with(QUEUE_SEARCH_LISTINGS_DLQ);
    }

    // Listing Events Queue
    @Bean
    public Binding listingReservationEvents(Queue listingEventsQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(listingEventsQueue).to(domainExchange).with("rental.reservation-requested.v1");
    }

    @Bean
    public Binding bindListingTenancyConfirmed(Queue listingEventsQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(listingEventsQueue).to(domainExchange).with("tenancy.confirmed.v1");
    }

    @Bean
    public Binding bindListingLandlordVerified(Queue listingEventsQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(listingEventsQueue).to(domainExchange).with("landlord.verified.v1");
    }

    @Bean
    public Binding bindListingDlq(Queue listingEventsDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(listingEventsDlq).to(deadLetterExchange).with(QUEUE_LISTING_EVENTS_DLQ);
    }

    // Notification Queue
    @Bean
    public Binding bindNotificationEvents(Queue notificationEventsQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(domainExchange).with("*.verified.v1");
    }

    @Bean
    public Binding bindNotificationPayments(Queue notificationEventsQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(domainExchange).with("payment.#");
    }

    @Bean
    public Binding bindNotificationDlq(Queue notificationEventsDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(notificationEventsDlq).to(deadLetterExchange).with(QUEUE_NOTIFICATION_EVENTS_DLQ);
    }


    // Rental Workflow Events Queue
    @Bean
    public Binding bindRentalWorkflow(Queue rentalWorkflowQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(rentalWorkflowQueue).to(domainExchange).with("listing.reserved.v1");
    }

    @Bean
    public Binding bindRentalReservationRejected(Queue rentalWorkflowQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(rentalWorkflowQueue).to(domainExchange).with("listing.reservation-rejected.v1");
    }

    @Bean
    public Binding bindRentalPaymentSucceeded(Queue rentalWorkflowQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(rentalWorkflowQueue).to(domainExchange).with("payment.succeeded.v1");
    }

    @Bean
    public Binding bindRentalPaymentFailed(Queue rentalWorkflowQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(rentalWorkflowQueue).to(domainExchange).with("payment.failed.v1");
    }

    @Bean
    public Binding bindRentalDlq(Queue rentalWorkflowDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(rentalWorkflowDlq).to(deadLetterExchange).with(QUEUE_RENTAL_WORKFLOW_DLQ);
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
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        template.setObservationEnabled(true);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(new SimpleMessageConverter());
        factory.setAcknowledgeMode(MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setObservationEnabled(true);
        return factory;
    }

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
