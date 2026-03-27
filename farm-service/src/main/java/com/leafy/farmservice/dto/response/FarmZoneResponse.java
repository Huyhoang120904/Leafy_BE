package com.leafy.farmservice.dto.response;

import com.leafy.farmservice.model.enums.FarmZoneStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FarmZoneResponse {
    private UUID id;
    private UUID farmPlotId;
    private String zoneName;
    private String zoneCode;
    private String description;
    private BigDecimal areaM2;
    private String soilType;
    private String cropType;
    private LocalDate plantingDate;
    private BigDecimal elevationM;
    private Map<String, Object> boundaryGeojson;
    private FarmZoneStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}