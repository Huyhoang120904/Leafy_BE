package com.leafy.plantmanagementservice.model;

import com.leafy.common.model.BaseModel;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

/**
 * Per-target progress entry for a broad-scope {@link PlantEvent}.
 *
 * <p>One entry is generated for every non-excluded zone or plant under the
 * parent event, depending on the parent's
 * {@link com.leafy.plantmanagementservice.model.enums.TrackingGranularity}.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "event_progress")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventProgress extends BaseModel {

    @MongoId(FieldType.OBJECT_ID)
    String id;

    /** Parent {@link PlantEvent} id. */
    @Indexed
    String eventId;

    /** Either {@code ZONE} or {@code PLANT}. */
    TargetType targetType;

    /** ID of the zone or plant being tracked. */
    @Indexed
    String targetId;

    /** Denormalized location refs (copied from parent or derived from target). */
    String farmPlotId;
    String farmZoneId;
    String plantId;

    /** Whether this individual target has completed the parent event's work. */
    boolean completed;

    LocalDateTime completedAt;

    /** Optional free-text note (e.g. who completed it, observation). */
    String note;

    public enum TargetType {
        ZONE,
        PLANT
    }
}
