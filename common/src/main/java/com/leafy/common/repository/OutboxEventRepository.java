package com.leafy.common.repository;

import com.leafy.common.model.kafka.EventType;
import com.leafy.common.model.kafka.OutboxEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public interface OutboxEventRepository extends MongoRepository<OutboxEvent, String> {
    
    @Query("{ 'status': 'PENDING' }")
    List<OutboxEvent> findPendingEvents();
    
    @Query("{ 'status': 'FAILED', 'retryCount': { $lt: ?0 } }")
    List<OutboxEvent> findFailedEventsForRetry(int maxRetries);
    
    List<OutboxEvent> findByStatusAndRetryCountGreaterThanEqual(OutboxEvent.OutboxEventStatus status, int retryCount);
    
    Optional<OutboxEvent> findTopByAggregateIdAndEventTypeOrderByCreatedAtDesc(String aggregateId, EventType eventType);
}
