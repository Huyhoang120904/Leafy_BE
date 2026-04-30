package com.leafy.farmservice.service.farmplot;

import com.leafy.farmservice.dto.request.farmplot.CreateFarmPlotRequest;
import com.leafy.farmservice.dto.request.farmplot.UpdateFarmPlotRequest;
import com.leafy.farmservice.dto.response.farmplot.FarmPlotResponse;
import com.leafy.farmservice.model.enums.FarmPlotStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FarmPlotService {
    FarmPlotResponse create(CreateFarmPlotRequest request);
    List<FarmPlotResponse> getByOwner(String ownerProfileId);
    List<FarmPlotResponse> getAllActive();
    Page<FarmPlotResponse> getFilteredPlots(String searchTerm, FarmPlotStatus status, String provinceCode, Double minAreaM2, Double maxAreaM2, Pageable pageable);
    FarmPlotResponse getById(String id);
    FarmPlotResponse update(String id, UpdateFarmPlotRequest request);
    void softDelete(String id);
}
