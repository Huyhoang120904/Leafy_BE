package com.leafy.plantmanagementservice.service.eventprogress;

import com.leafy.plantmanagementservice.dto.request.plantevent.EventProgressUpdateRequest;
import com.leafy.plantmanagementservice.dto.response.plantevent.EventProgressResponse;
import com.leafy.plantmanagementservice.model.PlantEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EventProgressService {

    /**
     * Generate per-target progress entries for a freshly-created broad-scope event.
     * No-op when {@code event.trackingGranularity} is {@code NONE} or the event
     * is plant-scoped.
     *
     * @return the created entries (empty if none generated).
     */
    List<EventProgressResponse> generateForEvent(PlantEvent event);

    Page<EventProgressResponse> getByEventId(String eventId, Pageable pageable);

    List<EventProgressResponse> getAllByEventId(String eventId);

    /** Update completion / note for a single progress entry, recomputing parent counters. */
    EventProgressResponse update(String progressId, EventProgressUpdateRequest request);

    /** Mark every progress entry for the given event as completed.
     * Returns the number of entries marked so the caller can update parent counters. */
    int completeAllByEventId(String eventId);

    /**
     * Idempotent generate-or-return: if progress entries already exist for the event
     * they are returned unchanged. Otherwise entries are generated from the event's
     * current scope. If {@code trackingGranularity} is {@code NONE} and the event has
     * a zone or plot scope, it is auto-set to {@code PLANT} before generation.
     *
     * @return the existing or newly created entries (empty when the event is plant-scoped with no zone/plot).
     */
    List<EventProgressResponse> generateOrRefresh(String eventId);

    /** Cascade delete all progress entries for an event. */
    void deleteByEventId(String eventId);
}
