package com.lagosrent.rental.infrastructure.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

    @Bean
    public Binding bindRentalWorkflow(Queue rentalWorkflowQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(rentalWorkflowQueue).to(domainExchange).with("listing.reserved.v1");
    }

    @Bean
    public Binding bindRentalDlq(Queue rentalWorkflowDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(rentalWorkflowDlq).to(deadLetterExchange).with(QUEUE_RENTAL_WORKFLOW_DLQ);
    }

    // JSON Message Converter
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        return template;
    }
}
