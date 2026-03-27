package com.leafy.farmservice.service;

import com.leafy.farmservice.dto.request.CreateFarmPlotRequest;
import com.leafy.farmservice.dto.request.UpdateFarmPlotRequest;
import com.leafy.farmservice.dto.response.FarmPlotResponse;
import java.util.List;
import java.util.UUID;

public interface FarmPlotService {
    FarmPlotResponse create(CreateFarmPlotRequest request);
    List<FarmPlotResponse> getByOwner(UUID ownerUserId);
    FarmPlotResponse getById(UUID id);
    FarmPlotResponse update(UUID id, UpdateFarmPlotRequest request);
    void softDelete(UUID id);
}