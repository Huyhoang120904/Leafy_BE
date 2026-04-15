package com.leafy.iotmetricscollectorservice.dto.alert_rule;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAlertRuleRequest {

    private UUID sensorTypeId;
    private UUID deviceId;
    private UUID zoneId;
    private UUID farmPlotId;
    private Double minThreshold;
    private Double maxThreshold;
    private String severity;
    private Integer cooldownMinutes;
    private Boolean notifyWeb;
    private Boolean notifyMobile;
    private Boolean enabled;
}
