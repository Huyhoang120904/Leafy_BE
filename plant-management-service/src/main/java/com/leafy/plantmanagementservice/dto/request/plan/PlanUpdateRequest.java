package com.leafy.plantmanagementservice.dto.request.plan;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanUpdateRequest {

    /** Custom name for the plan */
    String planName;

    /** Disease / pest name */
    String diseaseName;

    Double confidenceScore;

    String severityLevel;

    String urgency;

    // ── Plan metadata ─────────────────────────────────────────────────────────

    List<String> requiredInputs;

    List<String> safetyWarnings;

    String successIndicators;

    String estimatedCost;
}
