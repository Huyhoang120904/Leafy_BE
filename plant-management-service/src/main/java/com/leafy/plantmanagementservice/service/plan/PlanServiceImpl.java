package com.leafy.plantmanagementservice.service.plan;

import com.leafy.common.config.kafka.KafkaTopicProperties;
import com.leafy.common.enums.NotificationType;
import com.leafy.common.event.PlanApplyRequestedEvent;
import com.leafy.common.event.notification.RawNotificationEvent;
import com.leafy.common.exception.AppException;
import com.leafy.common.exception.ErrorCode;
import com.leafy.common.publisher.RawNotificationEventPublisher;
import com.leafy.common.utils.ServiceSecurityUtils;
import com.leafy.plantmanagementservice.client.ProfileServiceClient;
import com.leafy.plantmanagementservice.client.dto.ProfileSummary;
import com.leafy.plantmanagementservice.dto.request.plan.PlanApplyRequest;
import com.leafy.plantmanagementservice.dto.request.plan.PlanCreateRequest;
import com.leafy.plantmanagementservice.dto.request.plantevent.PlantEventCreateRequest;
import com.leafy.plantmanagementservice.dto.response.plan.AuthorInfo;
import com.leafy.plantmanagementservice.dto.response.plan.PlanResponse;
import com.leafy.plantmanagementservice.dto.response.plant.BulkOperationResult;
import com.leafy.plantmanagementservice.helper.ConsultingAccessHelper;
import com.leafy.plantmanagementservice.mapper.PlanMapper;
import com.leafy.plantmanagementservice.model.Plan;
import com.leafy.plantmanagementservice.model.enums.PlanStatus;
import com.leafy.plantmanagementservice.repository.PlanRepository;
import com.leafy.plantmanagementservice.service.plantevent.PlantEventService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlanServiceImpl implements PlanService {

    PlanRepository planRepository;
    PlanMapper planMapper;
    PlantEventService plantEventService;
    KafkaTemplate<String, Object> kafkaTemplate;
    KafkaTopicProperties kafkaTopicProperties;
    ConsultingAccessHelper consultingAccessHelper;
    ProfileServiceClient profileServiceClient;
    RawNotificationEventPublisher notificationPublisher;

    @Override
    @Transactional
    public PlanResponse createPlan(PlanCreateRequest request) {
        String userId = ServiceSecurityUtils.getCurrentAccountId();
        log.info("Creating Plan for userId={} disease={}", userId, request.getDiseaseName());

        // 1. Build entity (no id, userId, eventIds yet)
        String profileId = ServiceSecurityUtils.getCurrentProfileId();
        Plan plan = planMapper.toEntity(request);
        plan.setUserId(userId);
        plan.setCreatorId(profileId);
        plan.setOwnerId(profileId);
        plan.setPublic(request.getIsPublic() != null && request.getIsPublic());

        // 2. Persist the plan first so we have a real MongoDB _id for the events
        Plan saved = planRepository.save(plan);
        String realPlanId = saved.getId();

        // 3. Bulk-create scheduled plant events using the real plan _id
        List<String> eventIds = Collections.emptyList();
        if (!CollectionUtils.isEmpty(request.getSchedule())) {
            List<PlantEventCreateRequest> events = request.getSchedule();
            // Inject plant/farm scope and the real plan _id as sourcePlanId
            events.forEach(e -> {
                if (e.getPlantId() == null) e.setPlantId(request.getPlantId());
                if (e.getFarmPlotId() == null) e.setFarmPlotId(request.getFarmPlotId());
                if (e.getFarmZoneId() == null) e.setFarmZoneId(request.getFarmZoneId());
                e.setSourcePlanId(realPlanId);
            });
            eventIds = plantEventService.createEvents(events).stream()
                    .map(r -> r.getId())
                    .toList();
        }

        // 4. Patch the plan with the generated event IDs
        saved.setPlantEventIds(eventIds);
        saved = planRepository.save(saved);

        log.info("Plan created id={} with {} events", saved.getId(), eventIds.size());
        return enrich(planMapper.toResponse(saved));
    }

    @Override
    @Transactional
    public void applyPlan(String planId, PlanApplyRequest request) {
        log.info("Scheduling async apply for Plan id={} with startDate={}", planId, request.getStartDate());
        Plan plan = getPlanEntity(planId);

        if (CollectionUtils.isEmpty(plan.getPlantEventIds())) {
            log.warn("Plan id={} has no template events to apply", planId);
            return;
        }

        plan.setStatus(PlanStatus.APPLYING);
        planRepository.save(plan);

        PlanApplyRequestedEvent event = PlanApplyRequestedEvent.builder()
                .planId(planId)
                .startDate(request.getStartDate())
                .plantId(request.getPlantId())
                .farmZoneId(request.getFarmZoneId())
                .farmPlotId(request.getFarmPlotId())
                .trackingGranularity(request.getTrackingGranularity() == null ? null : request.getTrackingGranularity().name())
                .excludedPlantIds(request.getExcludedPlantIds())
                .excludedFarmZoneIds(request.getExcludedFarmZoneIds())
                .build();

        kafkaTemplate.send(kafkaTopicProperties.getSystemEvents().getPlanApplyRequested(), planId, event);
        log.info("Dispatched PlanApplyRequestedEvent for planId={}", planId);
    }

    @Override
    public PlanResponse getPlanById(String planId) {
        Plan plan = getPlanEntity(planId);
        // Allow access if plan is public, or the requester is the owner/creator
        if (!plan.isPublic()) {
            String profileId = ServiceSecurityUtils.getCurrentProfileId();
            String userId = ServiceSecurityUtils.getCurrentAccountId();
            boolean isOwner = profileId != null && profileId.equals(plan.getOwnerId());
            boolean isCreator = profileId != null && profileId.equals(plan.getCreatorId());
            // Legacy plans (created before ownerId/creatorId fields existed) only have userId
            boolean isUserMatch = userId != null && userId.equals(plan.getUserId());
            if (!isOwner && !isCreator && !isUserMatch) {
                throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
            }
        }
        return enrich(planMapper.toResponse(plan));
    }

    @Override
    public Page<PlanResponse> getPlansByCurrentUser(Pageable pageable) {
        String profileId = ServiceSecurityUtils.getCurrentProfileId();
        return enrichPage(planRepository.findByOwnerIdOrCreatorId(profileId, profileId, pageable)
                .map(planMapper::toResponse));
    }

    @Override
    public Page<PlanResponse> getPlansByCurrentUserAndStatus(PlanStatus status, Pageable pageable) {
        String profileId = ServiceSecurityUtils.getCurrentProfileId();
        return enrichPage(planRepository.findByOwnerIdAndStatus(profileId, status, pageable)
                .map(planMapper::toResponse));
    }

    @Override
    public Page<PlanResponse> getPlansByPlantId(String plantId, Pageable pageable) {
        return enrichPage(planRepository.findByPlantId(plantId, pageable)
                .map(planMapper::toResponse));
    }

    @Override
    public Page<PlanResponse> getPlansByFarmPlotId(String farmPlotId, Pageable pageable) {
        return enrichPage(planRepository.findByFarmPlotId(farmPlotId, pageable)
                .map(planMapper::toResponse));
    }

    @Override
    public Page<PlanResponse> getPlansByFarmZoneId(String farmZoneId, Pageable pageable) {
        return enrichPage(planRepository.findByFarmZoneId(farmZoneId, pageable)
                .map(planMapper::toResponse));
    }

    @Override
    @Transactional
    public PlanResponse updateStatus(String planId, PlanStatus newStatus) {
        log.info("Updating Plan id={} status → {}", planId, newStatus);
        Plan plan = getPlanEntity(planId);
        plan.setStatus(newStatus);
        return enrich(planMapper.toResponse(planRepository.save(plan)));
    }

    @Override
    @Transactional
    public void deletePlan(String planId) {
        log.info("Deleting Plan id={}", planId);
        Plan plan = getPlanEntity(planId);
        planRepository.delete(plan);
    }

    @Override
    public Page<PlanResponse> getConsultingPlans(String expertProfileId, String farmerProfileId, Pageable pageable) {
        log.info("Expert {} fetching consulting plans for farmer {}", expertProfileId, farmerProfileId);
        consultingAccessHelper.requireConsultingAccess(expertProfileId, farmerProfileId);
        return enrichPage(planRepository.findByOwnerId(farmerProfileId, pageable)
                .map(planMapper::toResponse));
    }

    @Override
    @Transactional
    public PlanResponse createConsultingPlan(String expertProfileId, String farmerProfileId, PlanCreateRequest request) {
        log.info("Expert {} creating consulting plan for farmer {}", expertProfileId, farmerProfileId);
        consultingAccessHelper.requireConsultingAccess(expertProfileId, farmerProfileId);

        String userId = ServiceSecurityUtils.getCurrentAccountId();
        Plan plan = planMapper.toEntity(request);
        plan.setUserId(userId);
        plan.setCreatorId(expertProfileId);
        plan.setOwnerId(farmerProfileId);  // owner is the farmer, not the expert
        plan.setPublic(request.getIsPublic() != null && request.getIsPublic());
        plan.setConsulted(true);

        // Persist first to get a real MongoDB _id for the events
        Plan saved = planRepository.save(plan);
        String realPlanId = saved.getId();

        List<String> eventIds = Collections.emptyList();
        if (!CollectionUtils.isEmpty(request.getSchedule())) {
            List<PlantEventCreateRequest> events = request.getSchedule();
            events.forEach(e -> {
                if (e.getPlantId() == null) e.setPlantId(request.getPlantId());
                if (e.getFarmPlotId() == null) e.setFarmPlotId(request.getFarmPlotId());
                if (e.getFarmZoneId() == null) e.setFarmZoneId(request.getFarmZoneId());
                e.setSourcePlanId(realPlanId);
            });
            eventIds = plantEventService.createEvents(events).stream()
                    .map(r -> r.getId())
                    .toList();
        }

        saved.setPlantEventIds(eventIds);
        saved = planRepository.save(saved);
        log.info("Consulting plan created id={} by expert={} for farmer={}", saved.getId(), expertProfileId, farmerProfileId);

        // ── Notify the farmer that an expert created a treatment plan for them ──
        publishConsultingPlanNotification(saved, expertProfileId, farmerProfileId);

        return enrich(planMapper.toResponse(saved));
    }

    /**
     * Fires PLAN_CONSULTING_CREATED notification to the farmer when an expert
     * creates a treatment plan on their behalf. Self-action guard prevents
     * notifying the actor (in case creator == owner for some reason).
     */
    private void publishConsultingPlanNotification(Plan plan, String expertProfileId, String farmerProfileId) {
        if (expertProfileId == null || farmerProfileId == null || expertProfileId.equals(farmerProfileId)) {
            return;
        }
        try {
            String actorName = expertProfileId;
            String actorAvatar = null;
            try {
                ProfileSummary expert = profileServiceClient.getProfileById(expertProfileId).getData();
                if (expert != null) {
                    if (expert.getFullName() != null) actorName = expert.getFullName();
                    actorAvatar = expert.getProfilePicture() != null
                            ? expert.getProfilePicture()
                            : expert.getAvatar();
                }
            } catch (Exception e) {
                log.warn("[Notification] Failed to resolve expert profile {} for consulting-plan notification: {}",
                        expertProfileId, e.getMessage());
            }

            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            if (plan.getPlanName() != null && !plan.getPlanName().isBlank()) {
                payload.put("planName", plan.getPlanName());
            }
            if (plan.getDiseaseName() != null && !plan.getDiseaseName().isBlank()) {
                payload.put("diseaseName", plan.getDiseaseName());
            }

            notificationPublisher.publish(RawNotificationEvent.builder()
                    .recipientId(farmerProfileId)
                    .actorId(expertProfileId)
                    .actorName(actorName)
                    .actorAvatar(actorAvatar)
                    .type(NotificationType.PLAN_CONSULTING_CREATED)
                    .referenceId(plan.getId())
                    .payload(payload)
                    .occurredAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("[Notification] Failed to publish consulting-plan notification: planId={}, expert={}, farmer={}",
                    plan.getId(), expertProfileId, farmerProfileId, e);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    @Override
    public Page<PlanResponse> getAllPlans(PlanStatus status, Pageable pageable) {
        log.info("Fetching all Plans, status={}", status);
        if (status != null) {
            return enrichPage(planRepository.findByStatus(status, pageable)
                    .map(planMapper::toResponse));
        }
        return enrichPage(planRepository.findAll(pageable)
                .map(planMapper::toResponse));
    }

    private Plan getPlanEntity(String planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));
    }

    @Override
    @Transactional
    public PlanResponse toggleVisibility(String planId) {
        Plan plan = getPlanEntity(planId);
        String profileId = ServiceSecurityUtils.getCurrentProfileId();
        boolean isOwner = profileId.equals(plan.getOwnerId());
        boolean isCreator = profileId.equals(plan.getCreatorId());
        if (!isOwner && !isCreator) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        boolean newValue = !plan.isPublic();
        log.info("Toggling Plan id={} visibility: {} → {}", planId, plan.isPublic(), newValue);
        plan.setPublic(newValue);
        return enrich(planMapper.toResponse(planRepository.save(plan)));
    }


    @Override
    @Transactional
    public BulkOperationResult bulkUpdateStatus(List<String> planIds, PlanStatus status) {
        log.info("Bulk updating {} plans to status={}", planIds.size(), status);
        int successCount = 0;
        List<String> failedIds = new ArrayList<>();
        for (String planId : planIds) {
            try {
                Plan plan = planRepository.findById(planId)
                        .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));
                plan.setStatus(status);
                planRepository.save(plan);
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to update status for plan id={}: {}", planId, e.getMessage());
                failedIds.add(planId);
            }
        }
        return BulkOperationResult.builder()
                .successCount(successCount)
                .failedCount(failedIds.size())
                .failedIds(failedIds)
                .build();
    }

    @Override
    @Transactional
    public BulkOperationResult bulkDelete(List<String> planIds) {
        log.info("Bulk deleting {} plans", planIds.size());
        int successCount = 0;
        List<String> failedIds = new ArrayList<>();
        for (String planId : planIds) {
            try {
                if (!planRepository.existsById(planId)) {
                    throw new AppException(ErrorCode.PLAN_NOT_FOUND);
                }
                planRepository.deleteById(planId);
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to delete plan id={}: {}", planId, e.getMessage());
                failedIds.add(planId);
            }
        }
        return BulkOperationResult.builder()
                .successCount(successCount)
                .failedCount(failedIds.size())
                .failedIds(failedIds)
                .build();
    }

    // ── Author enrichment ─────────────────────────────────────────────────────

    private PlanResponse enrich(PlanResponse response) {
        List<String> ids = Stream.of(response.getOwnerId(), response.getCreatorId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) return response;

        Map<String, AuthorInfo> profileMap = fetchProfileMap(ids);
        if (response.getOwnerId() != null)
            response.setOwnerInfo(profileMap.get(response.getOwnerId()));
        if (response.getCreatorId() != null)
            response.setCreatorInfo(profileMap.get(response.getCreatorId()));
        return response;
    }

    private Page<PlanResponse> enrichPage(Page<PlanResponse> page) {
        List<String> ids = page.getContent().stream()
                .flatMap(r -> Stream.of(r.getOwnerId(), r.getCreatorId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) return page;

        Map<String, AuthorInfo> profileMap = fetchProfileMap(ids);
        page.getContent().forEach(r -> {
            if (r.getOwnerId() != null) r.setOwnerInfo(profileMap.get(r.getOwnerId()));
            if (r.getCreatorId() != null) r.setCreatorInfo(profileMap.get(r.getCreatorId()));
        });
        return page;
    }

    private Map<String, AuthorInfo> fetchProfileMap(List<String> ids) {
        try {
            var apiResponse = profileServiceClient.getProfilesByIds(ids);
            if (apiResponse == null || apiResponse.getData() == null) return Map.of();
            return apiResponse.getData().stream()
                    .collect(Collectors.toMap(
                            ProfileSummary::getId,
                            p -> AuthorInfo.builder()
                                    .id(p.getId())
                                    .fullName(p.getFullName())
                                    .avatar(p.getAvatar() != null ? p.getAvatar() : p.getProfilePicture())
                                    .role(p.getRole())
                                    .specialty(p.getSpecialty())
                                    .isVerified(p.getIsVerified())
                                    .build(),
                            (a, b) -> a
                    ));
        } catch (Exception e) {
            log.warn("Failed to fetch author profiles for ids={}: {}", ids, e.getMessage());
            return Map.of();
        }
    }
}
