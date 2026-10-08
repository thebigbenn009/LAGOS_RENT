package com.lagosrent.payment.infrastructure.persistence;


import com.lagosrent.payment.domain.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    /**
     * Finds unpublished outbox records and locks them using PostgreSQL's FOR UPDATE SKIP LOCKED.
     * This allows multiple service replicas to run concurrently without publishing duplicate messages.
     */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE published_at IS NULL
            ORDER BY occurred_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> fetchUnpublishedEvents(@Param("batchSize") int batchSize);
}
