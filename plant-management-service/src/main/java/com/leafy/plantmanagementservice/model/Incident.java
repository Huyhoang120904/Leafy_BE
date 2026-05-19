package com.leafy.plantmanagementservice.model;

import com.leafy.common.model.BaseModel;
import com.leafy.plantmanagementservice.model.enums.IncidentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDate;

/**
 * Records a single disease-detection incident tied to a {@link PlanApply}.
 *
 * <p>Created automatically when a {@code HEALTH_RECOVERY} event is marked complete.
 * The {@code success} and {@code outcome} fields are updated by the user when they
 * complete the last remaining event of the apply.
 */
@Document(collection = "incidents")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Incident extends BaseModel {

    @MongoId(FieldType.OBJECT_ID)
    String id;

    // ── Plan linkage ──────────────────────────────────────────────────────────

    /** ID of the {@link PlanApply} this incident belongs to. */
    @Indexed
    String planApplyId;

    /** ID of the parent {@link Plan} template. */
    @Indexed
    String planId;

    // ── Context ──────────────────────────────────────────────────────────────

    /** Human-readable disease name (e.g. "Berry Borer", "Coffee Rust"). */
    String diseaseName;

    /** Target scope at time of detection. */
    String plantId;
    String farmZoneId;
    String farmPlotId;

    // ── Event linkage ────────────────────────────────────────────────────────

    /** ID of the DISEASE_DETECTED event that triggered this incident. */
    String detectedEventId;

    /** ID of the HEALTH_RECOVERY event that resolved this incident. Null until resolved. */
    String recoveredEventId;

    // ── Dates ────────────────────────────────────────────────────────────────

    /** Date of the DISEASE_DETECTED event. */
    LocalDate detectedDate;

    /** Date of the HEALTH_RECOVERY event. */
    LocalDate recoveredDate;

    // ── Outcome ──────────────────────────────────────────────────────────────

    /**
     * Resolution status — set automatically to {@code RESOLVED} when the
     * HEALTH_RECOVERY event is completed, or to {@code CANCELLED} if the
     * user cancels the apply. Updated to {@code FAILED} if the user marks
     * the plan failed at last-event completion.
     */
    @Builder.Default
    IncidentStatus outcome = null;

    /**
     * Whether the treatment plan succeeded from the user's perspective.
     * {@code null} until the user makes a decision at last-event completion.
     */
    Boolean success;
}
