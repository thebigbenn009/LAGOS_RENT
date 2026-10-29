package com.lagosrent.payment.infrastructure.messaging;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

import static java.time.Instant.now;

/**
 * Custom Micrometer MeterBinder exposing domain KPIs for the Transactional Outbox:
 * 1. lagosrent_outbox_pending_events (Gauge: count of unpublished events)
 * 2. lagosrent_outbox_oldest_event_age_seconds (Gauge: age of the oldest unpublished event in seconds)
 */
@Component
@RequiredArgsConstructor
public class OutboxMetricsBinder implements MeterBinder {
    private final JdbcClient jdbcClient;

    @Override
    public void bindTo(MeterRegistry registry) {
        // Gauge 1: Number of pending outbox events awaiting publication
        Gauge.builder("lagosrent.outbox.pending.events", this, OutboxMetricsBinder::getPendingOutboxCount)
                .description("Number of unpublished transactional outbox events")
                .tag("service", "payment-service")
                .register(registry);

        // Gauge 2: Age of the oldest pending event in seconds (stalled publishers)
        Gauge.builder("lagosrent.outbox.event.age.seconds", this, OutboxMetricsBinder::getOldestOutboxAgeSeconds)
                .description("Age in seconds of the oldest pending outbox event")
                .tag("service", "payment-service")
                .register(registry);

    }

    private double getPendingOutboxCount() {
        try {
            Long count = jdbcClient.sql("SELECT COUNT(*) FROM outbox_events WHERE published_at IS NULL")
                    .query(Long.class)
                    .single();
            return count.doubleValue();
        }
        catch (Exception e) {
            return 0.0;
        }
    }

    private double getOldestOutboxAgeSeconds() {
        try {
            Timestamp oldest = jdbcClient.sql("SELECT MIN(created_at) FROM outbox_events WHERE published_at IS NULL")
                    .query(Timestamp.class)
                    .optional()
                    .orElse(null);

            if (oldest == null) {
                return 0.0; // No pemding events
            }
            long ageSeconds = now().getEpochSecond() - oldest.toInstant().getEpochSecond();
            return Math.max(0.0, (double) ageSeconds);
        }
        catch (Exception e) {
            return 0.0;
        }
    }
}
