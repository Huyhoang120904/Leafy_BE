package com.leafy.plantmanagementservice.model;

import com.leafy.common.model.BaseModel;
import com.leafy.plantmanagementservice.model.enums.EventType;
import com.leafy.plantmanagementservice.model.enums.TrackingGranularity;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "plant_events")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlantEvent extends BaseModel {

    @MongoId(FieldType.OBJECT_ID)
    String id;

    // ── Relationships ────────────────────────────────────────────────────────
    @Indexed
    String plantId;

    @Indexed
    String farmPlotId;

    @Indexed
    String farmZoneId;

    // ── Core Event Fields ────────────────────────────────────────────────────
    EventType eventType;

    /** Short display label shown on the calendar tile. */
    String note;

    /** Full description including dosage, PPE, method, etc. */
    String description;

    /** Days from creation date (used by RAG planner; stored for reference). */
    Integer daysFromNow;

    /** Duration of the event window in days. */
    Integer durationDays;

    /** false = immediate / detected today; true = scheduled for the future. */
    boolean planned;

    // ── Calculated Dates (from planner) ──────────────────────────────────────
    LocalDate calculatedStartDate;
    LocalDate calculatedEndDate;

    // ── Chemical Application Safety Fields ───────────────────────────────────
    /**
     * Pre-Harvest Interval in days. Non-null only for TREATMENT_APPLICATION events.
     */
    Integer phiDays;

    /** Full PPE list required. Non-null only for TREATMENT_APPLICATION events. */
    String ppeRequired;

    /** MRL compliance note. Non-null only for TREATMENT_APPLICATION events. */
    String mrlNote;

    /** Estimated cost as a free-text string (e.g. "$10-$20" or "500,000 VND"). */
    String estimatedCost;

    // ── Source Tracking ───────────────────────────────────────────────────────
    /** ID of the Plan (MongoDB) that generated this event, if any. */
    String sourcePlanId;

    // ── Completion ────────────────────────────────────────────────────────────
    /**
     * Overall event-level completion flag, independent of individual task completion.
     * Toggled directly by the user to mark the whole event done.
     */
    boolean completed;

    /**
     * Optional list of sub-tasks belonging to this event.
     * Each task tracks its own completion status.
     * Stored as an embedded array inside the event document.
     */
    List<EventTask> tasks;

    // ── Progress Tracking (broad-scope events only) ──────────────────────────
    /**
     * How this event is tracked across child targets.
     * <ul>
     *   <li>{@code NONE} — no per-target tracking (plant-scope or untracked broad events).</li>
     *   <li>{@code ZONE} — one progress entry per zone in the parent farm plot.</li>
     *   <li>{@code PLANT} — one progress entry per plant in the parent plot/zone.</li>
     * </ul>
     */
    TrackingGranularity trackingGranularity;

    /** Plant IDs explicitly excluded from progress generation. */
    List<String> excludedPlantIds;

    /** Farm zone IDs explicitly excluded from progress generation. */
    List<String> excludedFarmZoneIds;

    /** Denormalized total number of progress entries generated for this event. */
    Integer progressTotal;

    /** Denormalized number of progress entries currently marked completed. */
    Integer progressCompleted;
}
