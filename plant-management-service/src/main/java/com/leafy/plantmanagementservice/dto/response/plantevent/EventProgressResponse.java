package com.leafy.plantmanagementservice.dto.response.plantevent;

import com.leafy.plantmanagementservice.model.EventProgress.TargetType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventProgressResponse {

    String id;
    String eventId;
    TargetType targetType;
    String targetId;
    String farmPlotId;
    String farmZoneId;
    String plantId;
    boolean completed;
    LocalDateTime completedAt;
    String note;

    LocalDateTime createdAt;
    LocalDateTime lastModifiedAt;
    String createdBy;
    String lastModifiedBy;
    boolean active;
}
