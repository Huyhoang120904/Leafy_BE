package com.leafy.plantmanagementservice.dto.response.plan;

import com.leafy.plantmanagementservice.model.enums.PlanStatus;
import com.leafy.plantmanagementservice.model.enums.TrackingGranularity;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanApplyResponse {

    String id;

    // ── Link to parent Plan ──────────────────────────────────────────────────
    String planId;
    String planName;
    String diseaseName;

    // ── Who applied ──────────────────────────────────────────────────────────
    String appliedById;

    // ── Target scope ─────────────────────────────────────────────────────────
    String plantId;
    String farmPlotId;
    String farmZoneId;
    String targetName;

    // ── Application parameters ───────────────────────────────────────────────
    LocalDate startDate;
    TrackingGranularity trackingGranularity;

    // ── Generated events ─────────────────────────────────────────────────────
    List<String> plantEventIds;

    /**
     * The ID of the last event in the treatment sequence.
     * When this event is completed, the frontend prompts the user to indicate
     * whether the treatment was successful.
     */
    String lastEventId;

    // ── Lifecycle ────────────────────────────────────────────────────────────
    PlanStatus status;

    /** Whether this application can be cancelled by the user. */
    Boolean canCancel;

    /** Outcome — true = succeeded, false = failed, null = unresolved. */
    Boolean success;

    // ── Audit fields ─────────────────────────────────────────────────────────
    LocalDateTime createdAt;
    LocalDateTime lastModifiedAt;
}
