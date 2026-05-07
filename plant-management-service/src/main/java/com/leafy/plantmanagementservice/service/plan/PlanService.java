package com.leafy.plantmanagementservice.service.plan;

import com.leafy.plantmanagementservice.dto.request.plan.PlanCreateRequest;
import com.leafy.plantmanagementservice.dto.response.plan.PlanResponse;
import com.leafy.plantmanagementservice.dto.response.plant.BulkOperationResult;
import com.leafy.plantmanagementservice.model.enums.PlanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PlanService {

    /**
     * Create a treatment plan and bulk-create all scheduled plant events atomically.
     * The authenticated user's ID is resolved from the security context.
     */
    PlanResponse createPlan(PlanCreateRequest request);

    /**
     * Applies an existing treatment plan to a target (plant, farmZone, or farmPlot).
     */
    void applyPlan(String planId, com.leafy.plantmanagementservice.dto.request.plan.PlanApplyRequest request);

    PlanResponse getPlanById(String planId);

    Page<PlanResponse> getPlansByCurrentUser(Pageable pageable);

    Page<PlanResponse> getPlansByCurrentUserAndStatus(PlanStatus status, Pageable pageable);

    Page<PlanResponse> getPlansByPlantId(String plantId, Pageable pageable);

    Page<PlanResponse> getPlansByFarmPlotId(String farmPlotId, Pageable pageable);

    Page<PlanResponse> getPlansByFarmZoneId(String farmZoneId, Pageable pageable);

    PlanResponse updateStatus(String planId, PlanStatus newStatus);

    Page<PlanResponse> getAllPlans(PlanStatus status, Pageable pageable);

    void deletePlan(String planId);

    PlanResponse createConsultingPlan(String expertProfileId, String farmerProfileId, PlanCreateRequest request);

    Page<PlanResponse> getConsultingPlans(String expertProfileId, String farmerProfileId, Pageable pageable);

    BulkOperationResult bulkUpdateStatus(List<String> planIds, PlanStatus status);

    BulkOperationResult bulkDelete(List<String> planIds);

    /**
     * Toggles the public/private visibility of a plan.
     * Only the owner or creator of the plan can change its visibility.
     */
    PlanResponse toggleVisibility(String planId);
}

