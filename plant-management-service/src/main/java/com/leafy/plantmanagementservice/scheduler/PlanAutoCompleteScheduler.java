package com.leafy.plantmanagementservice.scheduler;

import com.leafy.plantmanagementservice.model.Plan;
import com.leafy.plantmanagementservice.model.PlantEvent;
import com.leafy.plantmanagementservice.model.enums.PlanStatus;
import com.leafy.plantmanagementservice.repository.PlanRepository;
import com.leafy.plantmanagementservice.repository.PlantEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled job that automatically marks ACTIVE treatment plans as COMPLETED
 * when every applied event (identified by {@code sourcePlanId = plan.id})
 * has an end date (or start date when no end date) that is before today.
 *
 * <p>Runs daily at 01:00.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanAutoCompleteScheduler {

    private final PlanRepository planRepository;
    private final PlantEventRepository plantEventRepository;

    @Scheduled(cron = "0 0 1 * * *")
    public void autoCompletePlans() {
        LocalDate today = LocalDate.now();
        List<Plan> activePlans = planRepository.findByStatus(PlanStatus.ACTIVE);
        log.info("PlanAutoCompleteScheduler: checking {} ACTIVE plans against date {}", activePlans.size(), today);

        int completed = 0;
        for (Plan plan : activePlans) {
            try {
                List<PlantEvent> appliedEvents = plantEventRepository.findBySourcePlanId(plan.getId());
                if (appliedEvents.isEmpty()) {
                    // No events have been applied to this plan yet — skip
                    continue;
                }

                boolean allPast = appliedEvents.stream().allMatch(e -> {
                    // Prefer calculatedEndDate; fall back to calculatedStartDate
                    LocalDate checkDate = e.getCalculatedEndDate() != null
                            ? e.getCalculatedEndDate()
                            : e.getCalculatedStartDate();
                    return checkDate != null && checkDate.isBefore(today);
                });

                if (allPast) {
                    plan.setStatus(PlanStatus.COMPLETED);
                    planRepository.save(plan);
                    completed++;
                    log.info("Plan id={} auto-completed ({} events all past)", plan.getId(), appliedEvents.size());
                }
            } catch (Exception e) {
                log.warn("Error auto-completing plan id={}: {}", plan.getId(), e.getMessage());
            }
        }
        log.info("PlanAutoCompleteScheduler: completed {} plans", completed);
    }
}
