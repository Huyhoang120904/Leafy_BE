package com.leafy.farmservice.service.farmzone;

import com.leafy.farmservice.dto.request.farmzone.CreateFarmZoneRequest;
import com.leafy.farmservice.dto.request.farmzone.UpdateFarmZoneRequest;
import com.leafy.farmservice.dto.response.farmzone.FarmZoneResponse;
import com.leafy.farmservice.model.enums.FarmZoneStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FarmZoneService {
    FarmZoneResponse create(String farmPlotId, CreateFarmZoneRequest request);
    List<FarmZoneResponse> getByFarmPlot(String farmPlotId);
    List<FarmZoneResponse> getAllActive();
    Page<FarmZoneResponse> getFilteredZones(String searchTerm, FarmZoneStatus status, String cropType, String soilType, Double minAreaM2, Double maxAreaM2, Pageable pageable);
    FarmZoneResponse getById(String id);
    FarmZoneResponse update(String id, UpdateFarmZoneRequest request);
    void softDelete(String id);
}
