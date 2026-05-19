package com.leafy.plantmanagementservice.service.incident;

import com.leafy.plantmanagementservice.dto.response.plan.IncidentResponse;

public interface IncidentService {

    /**
     * Creates an Incident record when a HEALTH_RECOVERY event is marked complete.
     *
     * @param healthRecoveryEventId ID of the completed HEALTH_RECOVERY event
     * @param planApplyId          ID of the owning PlanApply
     * @return the created IncidentResponse
     */
    IncidentResponse createIncidentFromHealthRecovery(String healthRecoveryEventId, String planApplyId);

    /**
     * Finds the DISEASE_DETECTED event ID for a given PlanApply.
     *
     * @param planApplyId the apply to search within
     * @return the event ID, or null if none found
     */
    String findDiseaseDetectionEventId(String planApplyId);

    /**
     * Returns the Incident for a given PlanApply, if one exists.
     *
     * @param planApplyId the apply to look up
     * @return the IncidentResponse, or null
     */
    IncidentResponse getByPlanApplyId(String planApplyId);
}
