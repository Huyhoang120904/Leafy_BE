package com.leafy.plantmanagementservice.dto.response.plantevent;

import com.leafy.plantmanagementservice.model.enums.EventType;
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
public class PlantEventResponse {

    String id;
    String plantId;
    String farmPlotId;
    String farmZoneId;
    EventType eventType;
    String note;
    String description;
    Integer daysFromNow;
    Integer durationDays;
    boolean planned;
    LocalDate calculatedStartDate;
    LocalDate calculatedEndDate;
    Integer phiDays;
    String ppeRequired;
    String mrlNote;
    String estimatedCost;
    String sourcePlanId;

    boolean completed;
    List<EventTaskResponse> tasks;

    // Progress tracking metadata
    TrackingGranularity trackingGranularity;
    List<String> excludedPlantIds;
    List<String> excludedFarmZoneIds;
    Integer progressTotal;
    Integer progressCompleted;

    // BaseModel audit fields
    LocalDateTime createdAt;
    LocalDateTime lastModifiedAt;
    String createdBy;
    String lastModifiedBy;
    boolean active;
}
