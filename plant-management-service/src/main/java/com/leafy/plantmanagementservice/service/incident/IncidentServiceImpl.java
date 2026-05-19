package com.leafy.plantmanagementservice.service.incident;

import com.leafy.plantmanagementservice.dto.response.plan.IncidentResponse;
import com.leafy.plantmanagementservice.model.Incident;
import com.leafy.plantmanagementservice.model.PlantEvent;
import com.leafy.plantmanagementservice.model.enums.EventType;
import com.leafy.plantmanagementservice.model.enums.IncidentStatus;
import com.leafy.plantmanagementservice.repository.IncidentRepository;
import com.leafy.plantmanagementservice.repository.PlantEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final PlantEventRepository plantEventRepository;

    @Override
    public IncidentResponse createIncidentFromHealthRecovery(String healthRecoveryEventId, String planApplyId) {
        String detectedEventId = findDiseaseDetectionEventId(planApplyId);

        PlantEvent detectedEvent = null;
        if (detectedEventId != null) {
            detectedEvent = plantEventRepository.findById(detectedEventId).orElse(null);
        }

        PlantEvent recoveryEvent = plantEventRepository.findById(healthRecoveryEventId).orElse(null);

        Incident incident = Incident.builder()
                .planApplyId(planApplyId)
                .diseaseName(detectedEvent != null ? detectedEvent.getDescription() : null)
                .plantId(detectedEvent != null ? detectedEvent.getPlantId() : null)
                .farmZoneId(detectedEvent != null ? detectedEvent.getFarmZoneId() : null)
                .farmPlotId(detectedEvent != null ? detectedEvent.getFarmPlotId() : null)
                .detectedEventId(detectedEventId)
                .recoveredEventId(healthRecoveryEventId)
                .detectedDate(detectedEvent != null ? detectedEvent.getCalculatedStartDate() : null)
                .recoveredDate(recoveryEvent != null ? recoveryEvent.getCalculatedStartDate() : null)
                .outcome(IncidentStatus.RESOLVED)
                .build();

        incident = incidentRepository.save(incident);
        log.info("Created Incident id={} for planApplyId={}", incident.getId(), planApplyId);
        return toResponse(incident);
    }

    @Override
    public String findDiseaseDetectionEventId(String planApplyId) {
        List<PlantEvent> diseaseEvents = plantEventRepository
                .findByPlanApplyIdAndEventType(planApplyId, EventType.DISEASE_DETECTED);
        if (diseaseEvents.isEmpty()) {
            log.warn("No DISEASE_DETECTED event found for planApplyId={}", planApplyId);
            return null;
        }
        return diseaseEvents.get(0).getId();
    }

    @Override
    public IncidentResponse getByPlanApplyId(String planApplyId) {
        return incidentRepository.findByPlanApplyId(planApplyId)
                .map(this::toResponse)
                .orElse(null);
    }

    private IncidentResponse toResponse(Incident i) {
        return IncidentResponse.builder()
                .id(i.getId())
                .planApplyId(i.getPlanApplyId())
                .planId(i.getPlanId())
                .diseaseName(i.getDiseaseName())
                .plantId(i.getPlantId())
                .farmZoneId(i.getFarmZoneId())
                .farmPlotId(i.getFarmPlotId())
                .detectedEventId(i.getDetectedEventId())
                .recoveredEventId(i.getRecoveredEventId())
                .detectedDate(i.getDetectedDate())
                .recoveredDate(i.getRecoveredDate())
                .outcome(i.getOutcome())
                .success(i.getSuccess())
                .createdAt(i.getCreatedAt())
                .lastModifiedAt(i.getLastModifiedAt())
                .build();
    }
}
