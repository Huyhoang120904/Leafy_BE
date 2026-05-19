package com.leafy.plantmanagementservice.dto.response.plan;

import com.leafy.plantmanagementservice.model.enums.IncidentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IncidentResponse {

    String id;

    // Plan linkage
    String planApplyId;
    String planId;

    // Context
    String diseaseName;
    String plantId;
    String farmZoneId;
    String farmPlotId;

    // Event linkage
    String detectedEventId;
    String recoveredEventId;

    // Dates
    LocalDate detectedDate;
    LocalDate recoveredDate;

    // Outcome
    IncidentStatus outcome;
    Boolean success;

    // Audit
    LocalDateTime createdAt;
    LocalDateTime lastModifiedAt;
}
