package com.leafy.iotmetricscollectorservice.service;

import com.leafy.iotmetricscollectorservice.dto.alert_rule.AlertRuleResponse;
import com.leafy.iotmetricscollectorservice.dto.alert_rule.CreateAlertRuleRequest;
import com.leafy.iotmetricscollectorservice.dto.alert_rule.UpdateAlertRuleRequest;
import com.leafy.iotmetricscollectorservice.dto.common.PagedResponse;
import java.util.UUID;

public interface AlertRuleService {

    AlertRuleResponse createRule(UUID currentUserId, CreateAlertRuleRequest request);

    PagedResponse<AlertRuleResponse> listRules(
        UUID currentUserId,
        UUID sensorTypeId,
        UUID deviceId,
        UUID zoneId,
        UUID farmPlotId,
        Boolean enabled,
        Integer page,
        Integer size,
        String sortBy,
        String sortDir
    );

    AlertRuleResponse getRule(UUID currentUserId, UUID ruleId);

    AlertRuleResponse updateRule(UUID currentUserId, UUID ruleId, UpdateAlertRuleRequest request);

    AlertRuleResponse updateRuleEnabled(UUID currentUserId, UUID ruleId, Boolean enabled);

    void deleteRule(UUID currentUserId, UUID ruleId);
}
