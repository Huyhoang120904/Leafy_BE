package com.leafy.plantmanagementservice.dto.response.plan;

import com.leafy.plantmanagementservice.model.enums.PlanStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanResponse {

    String id;

    // ── Source tracking ───────────────────────────────────────────────────────
    String creatorId;
    String ownerId;
    String ragPlanId;
    String planName;
    String question;
    String source;

    // ── Plant / Farm scope ────────────────────────────────────────────────────
    String plantId;
    String farmPlotId;
    String farmZoneId;

    // ── Diagnosis ─────────────────────────────────────────────────────────────
    String diseaseName;
    Double confidenceScore;
    String severityLevel;
    String urgency;

    // ── Plan metadata ─────────────────────────────────────────────────────────
    List<String> requiredInputs;
    List<String> safetyWarnings;
    String successIndicators;
    String estimatedCost;

    // ── Generated events ──────────────────────────────────────────────────────
    List<String> plantEventIds;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    PlanStatus status;
    Integer applyCount;

    // ── Visibility ────────────────────────────────────────────────────────────
    Boolean isPublic;

    // ── Consulting ───────────────────────────────────────────────────────────
    Boolean isConsulted;

    // ── Author info (enriched from profile-service) ───────────────────────────
    AuthorInfo ownerInfo;
    AuthorInfo creatorInfo;

    // ── Audit fields (BaseModel) ──────────────────────────────────────────────
    LocalDateTime createdAt;
    LocalDateTime lastModifiedAt;
    String createdBy;
    String lastModifiedBy;
    boolean active;
}
