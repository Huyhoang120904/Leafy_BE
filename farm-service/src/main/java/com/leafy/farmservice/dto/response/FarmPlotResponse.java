package com.leafy.farmservice.dto.response;

import com.leafy.farmservice.model.enums.FarmPlotStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FarmPlotResponse {
    private UUID id;
    private UUID ownerUserId;
    private String name;
    private String code;
    private String description;
    private BigDecimal areaM2;
    private String addressLine;
    private String provinceCode;
    private String districtCode;
    private String wardCode;
    private Double latitude;
    private Double longitude;
    private Map<String, Object> boundaryGeojson;
    private FarmPlotStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}