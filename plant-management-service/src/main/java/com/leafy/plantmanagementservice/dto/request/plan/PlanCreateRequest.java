package com.leafy.plantmanagementservice.dto.request.plan;

import com.leafy.plantmanagementservice.dto.request.plan.EmbeddedPlanEventRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanCreateRequest {

    /** UUID from the RAG service PlanDoc — links both records. */
    String ragPlanId;

    /** Original natural-language question the user sent to the RAG service. */
    String question;

    /** Custom name for the plan */
    String planName;

    @Pattern(regexp = "^(websearch|documents)$", message = "source must be one of: websearch, documents")
    String source;

    // ── Plant / Farm scope ────────────────────────────────────────────────────

    String plantId;
    String farmPlotId;
    String farmZoneId;

    // ── Diagnosis ─────────────────────────────────────────────────────────────

    @NotBlank(message = "diseaseName is required")
    String diseaseName;

    Double confidenceScore;

    String severityLevel;

    String urgency;

    // ── Plan metadata ─────────────────────────────────────────────────────────

    List<String> requiredInputs;

    List<String> safetyWarnings;

    String successIndicators;

    String estimatedCost;

    // ── Schedule ──────────────────────────────────────────────────────────────

    /**
     * Ordered list of template events to embed in the plan document.
     * Scope (plantId, farmPlotId, farmZoneId) is omitted here — it is resolved at apply time.
     */
    @Valid
    List<EmbeddedPlanEventRequest> schedule;

    // ── Visibility ────────────────────────────────────────────────────────────

    /** If true, this plan will be visible to all authenticated users. Defaults to false (private). */
    Boolean isPublic;
}
