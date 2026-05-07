package com.leafy.plantmanagementservice.repository;

import com.leafy.plantmanagementservice.model.EventProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventProgressRepository extends MongoRepository<EventProgress, String> {

    Page<EventProgress> findByEventId(String eventId, Pageable pageable);

    List<EventProgress> findByEventId(String eventId);

    long countByEventId(String eventId);

    long countByEventIdAndCompleted(String eventId, boolean completed);

    void deleteByEventId(String eventId);
}
