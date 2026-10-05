package com.lagosrent.search.infrastructure.persistence;

import com.lagosrent.search.domain.model.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, ProcessedMessage.ProcessedMessageId> {
    boolean existsByIdEventIdAndIdConsumerName(UUID eventId, String consumerName);
}
