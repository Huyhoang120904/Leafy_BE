package com.leafy.plantmanagementservice.service.eventprogress;

import com.leafy.common.exception.AppException;
import com.leafy.common.exception.ErrorCode;
import com.leafy.plantmanagementservice.dto.request.plantevent.EventProgressUpdateRequest;
import com.leafy.plantmanagementservice.dto.response.plantevent.EventProgressResponse;
import com.leafy.plantmanagementservice.mapper.EventProgressMapper;
import com.leafy.plantmanagementservice.model.EventProgress;
import com.leafy.plantmanagementservice.model.FarmZone;
import com.leafy.plantmanagementservice.model.Plant;
import com.leafy.plantmanagementservice.model.PlantEvent;
import com.leafy.plantmanagementservice.model.enums.TrackingGranularity;
import com.leafy.plantmanagementservice.repository.EventProgressRepository;
import com.leafy.plantmanagementservice.repository.FarmZoneRepository;
import com.leafy.plantmanagementservice.repository.PlantEventRepository;
import com.leafy.plantmanagementservice.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventProgressServiceImpl implements EventProgressService {

    private final EventProgressRepository eventProgressRepository;
    private final PlantEventRepository plantEventRepository;
    private final PlantRepository plantRepository;
    private final FarmZoneRepository farmZoneRepository;
    private final EventProgressMapper eventProgressMapper;

    @Override
    @Transactional
    public List<EventProgressResponse> generateForEvent(PlantEvent event) {
        TrackingGranularity granularity = event.getTrackingGranularity();
        boolean hasPlot = StringUtils.hasText(event.getFarmPlotId());
        boolean hasZone = StringUtils.hasText(event.getFarmZoneId());
        boolean hasPlant = StringUtils.hasText(event.getPlantId());

        // Plant-scoped events: create a single progress entry for that specific plant.
        if (hasPlant) {
            if (granularity == null || granularity == TrackingGranularity.NONE) {
                log.debug("Skipping progress generation: plant-scoped event id={} has no tracking granularity", event.getId());
                return Collections.emptyList();
            }
            Plant plant = plantRepository.findById(event.getPlantId()).orElse(null);
            if (plant == null) {
                log.warn("Cannot generate progress: plant id={} not found for event id={}", event.getPlantId(), event.getId());
                return Collections.emptyList();
            }
            EventProgress entry = buildPlantEntry(event, plant);
            EventProgress saved = eventProgressRepository.save(entry);
            event.setProgressTotal(1);
            event.setProgressCompleted(0);
            plantEventRepository.save(event);
            log.debug("Generated 1 progress entry for plant-scoped event id={}", event.getId());
            return eventProgressMapper.toResponseList(List.of(saved));
        }

        // Default granularity for zone-scoped events is PLANT.
        if (hasZone && !hasPlot) {
            // zone scope (no plot id) — uncommon but handle via zone-only resolution
        }
        if (granularity == null || granularity == TrackingGranularity.NONE) {
            log.debug("No tracking granularity set for event id={}; skipping", event.getId());
            return Collections.emptyList();
        }

        Set<String> excludedPlantIds = toSet(event.getExcludedPlantIds());
        Set<String> excludedZoneIds = toSet(event.getExcludedFarmZoneIds());

        List<EventProgress> entries = new ArrayList<>();

        if (hasZone) {
            // ZONE scope: only PLANT granularity is meaningful.
            if (granularity != TrackingGranularity.PLANT) {
                log.warn("Zone-scoped event id={} requested granularity={}; forcing PLANT", event.getId(), granularity);
                event.setTrackingGranularity(TrackingGranularity.PLANT);
            }
            List<Plant> plants = plantRepository.findByFarmZoneId(event.getFarmZoneId());
            for (Plant plant : plants) {
                if (excludedPlantIds.contains(plant.getId())) {
                    continue;
                }
                entries.add(buildPlantEntry(event, plant));
            }
        } else if (hasPlot) {
            if (granularity == TrackingGranularity.ZONE) {
                List<FarmZone> zones = farmZoneRepository.findByFarmPlotIdAndActiveTrue(event.getFarmPlotId());
                for (FarmZone zone : zones) {
                    if (excludedZoneIds.contains(zone.getId())) {
                        continue;
                    }
                    entries.add(buildZoneEntry(event, zone));
                }
            } else { // PLANT granularity
                List<Plant> plants = plantRepository.findByFarmPlotIdIn(List.of(event.getFarmPlotId()));
                for (Plant plant : plants) {
                    if (excludedPlantIds.contains(plant.getId())) {
                        continue;
                    }
                    if (StringUtils.hasText(plant.getFarmZoneId()) && excludedZoneIds.contains(plant.getFarmZoneId())) {
                        continue;
                    }
                    entries.add(buildPlantEntry(event, plant));
                }
            }
        } else {
            log.warn("Cannot generate progress: event id={} has no plot/zone/plant scope", event.getId());
            return Collections.emptyList();
        }

        if (entries.isEmpty()) {
            log.info("No progress entries generated for event id={}", event.getId());
            // Still update counters to make state consistent.
            event.setProgressTotal(0);
            event.setProgressCompleted(0);
            plantEventRepository.save(event);
            return Collections.emptyList();
        }

        List<EventProgress> saved = eventProgressRepository.saveAll(entries);

        // Update denormalized counters on parent.
        event.setProgressTotal(saved.size());
        event.setProgressCompleted(0);
        plantEventRepository.save(event);

        log.info("Generated {} progress entries for event id={} (granularity={})",
                saved.size(), event.getId(), event.getTrackingGranularity());
        return eventProgressMapper.toResponseList(saved);
    }

    @Override
    @Transactional
    public int completeAllByEventId(String eventId) {
        List<EventProgress> entries = eventProgressRepository.findByEventId(eventId);
        if (entries.isEmpty()) {
            return 0;
        }
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        entries.forEach(e -> {
            e.setCompleted(true);
            e.setCompletedAt(now);
        });
        eventProgressRepository.saveAll(entries);
        log.info("Marked {} progress entries complete for event id={}", entries.size(), eventId);
        return entries.size();
    }

    @Override
    public Page<EventProgressResponse> getByEventId(String eventId, Pageable pageable) {
        return eventProgressRepository.findByEventId(eventId, pageable)
                .map(eventProgressMapper::toResponse);
    }

    @Override
    public List<EventProgressResponse> getAllByEventId(String eventId) {
        return eventProgressMapper.toResponseList(eventProgressRepository.findByEventId(eventId));
    }

    @Override
    @Transactional
    public EventProgressResponse update(String progressId, EventProgressUpdateRequest request) {
        EventProgress entry = eventProgressRepository.findById(progressId)
                .orElseThrow(() -> new AppException(ErrorCode.PLANT_EVENT_NOT_FOUND));

        boolean changed = false;
        if (request.getCompleted() != null && request.getCompleted() != entry.isCompleted()) {
            entry.setCompleted(request.getCompleted());
            entry.setCompletedAt(request.getCompleted() ? LocalDateTime.now() : null);
            changed = true;
        }
        if (request.getNote() != null) {
            entry.setNote(request.getNote());
        }

        EventProgress saved = eventProgressRepository.save(entry);

        if (changed) {
            recomputeParentCounters(entry.getEventId());
        }

        return eventProgressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public List<EventProgressResponse> generateOrRefresh(String eventId) {
        PlantEvent event = plantEventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.PLANT_EVENT_NOT_FOUND));

        // Return existing entries without modification (idempotent).
        List<EventProgress> existing = eventProgressRepository.findByEventId(eventId);
        if (!existing.isEmpty()) {
            log.info("generateOrRefresh: {} existing entries for event id={}", existing.size(), eventId);
            return eventProgressMapper.toResponseList(existing);
        }

        // Auto-set granularity when it is NONE or null and the event has a broad scope.
        boolean hasPlot = org.springframework.util.StringUtils.hasText(event.getFarmPlotId());
        boolean hasZone = org.springframework.util.StringUtils.hasText(event.getFarmZoneId());
        boolean hasPlant = org.springframework.util.StringUtils.hasText(event.getPlantId());

        if (!hasPlant && (hasPlot || hasZone)) {
            if (event.getTrackingGranularity() == null || event.getTrackingGranularity() == TrackingGranularity.NONE) {
                event.setTrackingGranularity(TrackingGranularity.PLANT);
                plantEventRepository.save(event);
                log.info("generateOrRefresh: auto-set granularity=PLANT for event id={}", eventId);
            }
        }

        return generateForEvent(event);
    }

    @Override
    @Transactional
    public void deleteByEventId(String eventId) {
        eventProgressRepository.deleteByEventId(eventId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void recomputeParentCounters(String eventId) {
        long total = eventProgressRepository.countByEventId(eventId);
        long completed = eventProgressRepository.countByEventIdAndCompleted(eventId, true);
        plantEventRepository.findById(eventId).ifPresent(parent -> {
            parent.setProgressTotal((int) total);
            parent.setProgressCompleted((int) completed);
            // Auto-complete parent when all entries are done.
            if (total > 0 && total == completed) {
                parent.setCompleted(true);
            } else if (parent.isCompleted() && completed < total) {
                parent.setCompleted(false);
            }
            plantEventRepository.save(parent);
        });
    }

    private EventProgress buildZoneEntry(PlantEvent event, FarmZone zone) {
        return EventProgress.builder()
                .eventId(event.getId())
                .targetType(EventProgress.TargetType.ZONE)
                .targetId(zone.getId())
                .farmPlotId(event.getFarmPlotId())
                .farmZoneId(zone.getId())
                .completed(false)
                .build();
    }

    private EventProgress buildPlantEntry(PlantEvent event, Plant plant) {
        return EventProgress.builder()
                .eventId(event.getId())
                .targetType(EventProgress.TargetType.PLANT)
                .targetId(plant.getId())
                .farmPlotId(plant.getFarmPlotId())
                .farmZoneId(plant.getFarmZoneId())
                .plantId(plant.getId())
                .completed(false)
                .build();
    }

    private static Set<String> toSet(List<String> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(list);
    }
}
