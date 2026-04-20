package com.leafy.plantmanagementservice.repository;

import com.leafy.plantmanagementservice.model.PlantEvent;
import com.leafy.plantmanagementservice.model.enums.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlantEventRepositoryCustom {

    /**
     * Admin multi-criteria filter for plant events.
     *
     * @param eventType  optional – filter by event type
     * @param planned    optional – true = scheduled, false = immediate/detected, null = all
     * @param farmPlotId optional – filter by farm plot ID
     * @param farmZoneId optional – filter by farm zone ID
     * @param pageable   pagination / sorting
     */
    Page<PlantEvent> findAllByFilters(
            EventType eventType,
            Boolean planned,
            String farmPlotId,
            String farmZoneId,
            Pageable pageable
    );
}
