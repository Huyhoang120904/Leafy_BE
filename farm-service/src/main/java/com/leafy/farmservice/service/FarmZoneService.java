package com.leafy.farmservice.service;

import com.leafy.farmservice.dto.request.CreateFarmZoneRequest;
import com.leafy.farmservice.dto.request.UpdateFarmZoneRequest;
import com.leafy.farmservice.dto.response.FarmZoneResponse;
import java.util.List;
import java.util.UUID;

public interface FarmZoneService {
    FarmZoneResponse create(UUID farmPlotId, CreateFarmZoneRequest request);
    List<FarmZoneResponse> getByFarmPlot(UUID farmPlotId);
    FarmZoneResponse getById(UUID id);
    FarmZoneResponse update(UUID id, UpdateFarmZoneRequest request);
    void softDelete(UUID id);
}