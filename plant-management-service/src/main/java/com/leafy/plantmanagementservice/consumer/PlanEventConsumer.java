package com.leafy.plantmanagementservice.consumer;

import com.leafy.common.config.kafka.KafkaTopicProperties;
import com.leafy.common.event.PlanAppliedEvent;
import com.leafy.common.event.PlanApplyRequestedEvent;
import com.leafy.plantmanagementservice.dto.request.plantevent.PlantEventCreateRequest;
import com.leafy.plantmanagementservice.model.Plan;
import com.leafy.plantmanagementservice.model.Plant;
import com.leafy.plantmanagementservice.model.PlantEvent;
import com.leafy.plantmanagementservice.model.enums.PlanStatus;
import com.leafy.plantmanagementservice.model.enums.TrackingGranularity;
import com.leafy.plantmanagementservice.repository.PlanRepository;
import com.leafy.plantmanagementservice.repository.PlantEventRepository;
import com.leafy.plantmanagementservice.repository.PlantRepository;
import com.leafy.plantmanagementservice.service.plantevent.PlantEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlanEventConsumer {

    private final PlanRepository planRepository;
    private final PlantRepository plantRepository;
    private final PlantEventRepository plantEventRepository;
    private final PlantEventService plantEventService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties kafkaTopicProperties;

    @KafkaListener(topics = "#{kafkaTopicProperties.systemEvents.planApplyRequested}", groupId = "${spring.application.name}-group")
    public void handlePlanApplyRequested(@Payload PlanApplyRequestedEvent event) {
        String planId = event.getPlanId();
        log.info("Received PlanApplyRequestedEvent for planId={}", planId);

        Plan plan = planRepository.findById(planId).orElse(null);
        if (plan == null) {
            log.error("Plan not found for id={}, skipping apply", planId);
            return;
        }

        List<PlantEvent> templateEvents = (List<PlantEvent>) plantEventRepository.findAllById(plan.getPlantEventIds());
        if (templateEvents.isEmpty()) {
            log.warn("No template events found for planId={}, skipping apply", planId);
            return;
        }

        boolean hasPlantId = StringUtils.hasText(event.getPlantId());
        boolean hasZoneId = StringUtils.hasText(event.getFarmZoneId());
        boolean hasPlotId = StringUtils.hasText(event.getFarmPlotId());

        if (!hasPlantId && !hasZoneId && !hasPlotId) {
            log.warn("PlanApplyRequestedEvent for planId={} has no scope set; skipping", planId);
            plan.setStatus(PlanStatus.PENDING);
            planRepository.save(plan);
            return;
        }

        int createdCount;
        if (hasPlantId) {
            createdCount = applyPlantScope(planId, event, templateEvents);
        } else {
            createdCount = applyBroadScope(planId, event, templateEvents, hasZoneId);
        }

        if (createdCount == 0) {
            log.warn("No events created for planId={}", planId);
            plan.setStatus(PlanStatus.PENDING);
            planRepository.save(plan);
            return;
        }

        plan.setStatus(PlanStatus.ACTIVE);
        plan.setApplyCount(plan.getApplyCount() == null ? 1 : plan.getApplyCount() + 1);
        planRepository.save(plan);

        kafkaTemplate.send(kafkaTopicProperties.getSystemEvents().getPlanApplied(), planId, new PlanAppliedEvent(planId));
        log.info("Plan id={} is now ACTIVE with applyCount={}", planId, plan.getApplyCount());
    }

    /**
     * PLANT scope: keep legacy behaviour — one PlantEvent per template against the single target plant.
     */
    private int applyPlantScope(String planId, PlanApplyRequestedEvent event, List<PlantEvent> templateEvents) {
        Plant target = plantRepository.findById(event.getPlantId()).orElse(null);
        if (target == null) {
            log.warn("Target plant id={} not found for planId={}", event.getPlantId(), planId);
            return 0;
        }
        List<PlantEventCreateRequest> newEvents = new ArrayList<>();
        for (PlantEvent template : templateEvents) {
            newEvents.add(buildRequest(template, planId, event.getStartDate(),
                    target.getId(), target.getFarmPlotId(), target.getFarmZoneId(),
                    null, null, null));
        }
        plantEventService.createEvents(newEvents);
        log.info("Created {} plant-scope events for planId={} plantId={}", newEvents.size(), planId, target.getId());
        return newEvents.size();
    }

    /**
     * FARM or ZONE scope: enumerate every plant in the target scope and create one
     * PlantEvent per plant per template.  Each per-plant event is then handed off to
     * PlantEventService.createEvents(), which calls EventProgressService.generateForEvent()
     * on every event so progress tracking is wired up automatically.
     */
    private int applyBroadScope(String planId, PlanApplyRequestedEvent event,
                                 List<PlantEvent> templateEvents, boolean hasZoneId) {

        // Resolve the target plants.
        List<Plant> plants = hasZoneId
                ? plantRepository.findByFarmZoneId(event.getFarmZoneId())
                : plantRepository.findByFarmPlotIdIn(List.of(event.getFarmPlotId()));

        if (plants.isEmpty()) {
            log.warn("applyBroadScope: no plants found for planId={} (zoneScope={} id={})",
                    planId, hasZoneId, hasZoneId ? event.getFarmZoneId() : event.getFarmPlotId());
            return 0;
        }

        // Apply exclusion lists.
        java.util.Set<String> excludedPlantIds = event.getExcludedPlantIds() != null
                ? new java.util.HashSet<>(event.getExcludedPlantIds()) : java.util.Collections.emptySet();
        java.util.Set<String> excludedZoneIds = event.getExcludedFarmZoneIds() != null
                ? new java.util.HashSet<>(event.getExcludedFarmZoneIds()) : java.util.Collections.emptySet();

        plants = plants.stream()
                .filter(p -> !excludedPlantIds.contains(p.getId()))
                .filter(p -> !StringUtils.hasText(p.getFarmZoneId()) || !excludedZoneIds.contains(p.getFarmZoneId()))
                .collect(java.util.stream.Collectors.toList());

        if (plants.isEmpty()) {
            log.warn("applyBroadScope: all plants excluded for planId={}", planId);
            return 0;
        }

        // Build one request per plant × template and batch-create.
        // trackingGranularity = PLANT so generateForEvent creates an EventProgress entry
        // for each resulting plant-scoped event.
        List<PlantEventCreateRequest> requests = new ArrayList<>();
        for (Plant plant : plants) {
            for (PlantEvent template : templateEvents) {
                requests.add(buildRequest(template, planId, event.getStartDate(),
                        plant.getId(), plant.getFarmPlotId(), plant.getFarmZoneId(),
                        TrackingGranularity.PLANT, null, null));
            }
        }

        plantEventService.createEvents(requests);
        log.info("Created {} per-plant events for planId={} (zoneScope={}, plants={}, templates={})",
                requests.size(), planId, hasZoneId, plants.size(), templateEvents.size());
        return requests.size();
    }

    private PlantEventCreateRequest buildRequest(PlantEvent template, String planId, LocalDate startDate,
                                                   String plantId, String farmPlotId, String farmZoneId,
                                                   TrackingGranularity granularity,
                                                   List<String> excludedPlantIds,
                                                   List<String> excludedFarmZoneIds) {
        PlantEventCreateRequest req = PlantEventCreateRequest.builder()
                .plantId(plantId)
                .farmPlotId(farmPlotId)
                .farmZoneId(farmZoneId)
                .eventType(template.getEventType())
                .note(template.getNote())
                .description(template.getDescription())
                .daysFromNow(template.getDaysFromNow())
                .durationDays(template.getDurationDays())
                .isPlanned(true)
                .phiDays(template.getPhiDays())
                .ppeRequired(template.getPpeRequired())
                .mrlNote(template.getMrlNote())
                .estimatedCost(template.getEstimatedCost())
                .sourcePlanId(planId)
                .trackingGranularity(granularity)
                .excludedPlantIds(excludedPlantIds)
                .excludedFarmZoneIds(excludedFarmZoneIds)
                .build();

        if (template.getDaysFromNow() != null && startDate != null) {
            LocalDate calcStart = startDate.plusDays(template.getDaysFromNow());
            req.setCalculatedStartDate(calcStart);
            if (template.getDurationDays() != null) {
                req.setCalculatedEndDate(calcStart.plusDays(template.getDurationDays()));
            }
        }
        return req;
    }

    @KafkaListener(topics = "#{kafkaTopicProperties.systemEvents.planApplied}", groupId = "${spring.application.name}-group")
    public void handlePlanApplied(@Payload PlanAppliedEvent event) {
        log.info("Plan successfully applied: planId={}", event.getPlanId());
    }
}
