package com.leafy.farmservice.service.impl;

import com.leafy.farmservice.dto.request.CreateFarmZoneRequest;
import com.leafy.farmservice.dto.request.UpdateFarmZoneRequest;
import com.leafy.farmservice.dto.response.FarmZoneResponse;
import com.leafy.farmservice.exception.BadRequestException;
import com.leafy.farmservice.exception.ResourceNotFoundException;
import com.leafy.farmservice.mapper.FarmMapper;
import com.leafy.farmservice.model.FarmPlot;
import com.leafy.farmservice.model.FarmZone;
import com.leafy.farmservice.repository.FarmPlotRepository;
import com.leafy.farmservice.repository.FarmZoneRepository;
import com.leafy.farmservice.service.FarmZoneService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FarmZoneServiceImpl implements FarmZoneService {

    private final FarmZoneRepository farmZoneRepository;
    private final FarmPlotRepository farmPlotRepository;

    @Override
    @Transactional
    public FarmZoneResponse create(UUID farmPlotId, CreateFarmZoneRequest request) {
        FarmPlot farmPlot = getActiveFarmPlot(farmPlotId);

        validateCreateRequest(request);

        if (farmZoneRepository.existsByFarmPlotIdAndZoneNameAndDeletedAtIsNull(farmPlotId, request.getZoneName())) {
            throw new BadRequestException("Zone name already exists in this farm plot");
        }

        FarmZone farmZone = new FarmZone();
        farmZone.setFarmPlot(farmPlot);
        farmZone.setZoneName(request.getZoneName());
        farmZone.setZoneCode(request.getZoneCode());
        farmZone.setDescription(request.getDescription());
        farmZone.setAreaM2(request.getAreaM2());
        farmZone.setSoilType(request.getSoilType());
        farmZone.setCropType(request.getCropType());
        farmZone.setPlantingDate(request.getPlantingDate());
        farmZone.setElevationM(request.getElevationM());
        farmZone.setBoundaryGeojson(request.getBoundaryGeojson());

        return FarmMapper.toFarmZoneResponse(farmZoneRepository.save(farmZone));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmZoneResponse> getByFarmPlot(UUID farmPlotId) {
        return farmZoneRepository.findByFarmPlotIdAndDeletedAtIsNull(farmPlotId)
                .stream()
                .map(FarmMapper::toFarmZoneResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FarmZoneResponse getById(UUID id) {
        FarmZone farmZone = getActiveFarmZone(id);
        return FarmMapper.toFarmZoneResponse(farmZone);
    }

    @Override
    @Transactional
    public FarmZoneResponse update(UUID id, UpdateFarmZoneRequest request) {
        FarmZone farmZone = getActiveFarmZone(id);

        if (request.getZoneName() != null
                && !request.getZoneName().equals(farmZone.getZoneName())
                && farmZoneRepository.existsByFarmPlotIdAndZoneNameAndDeletedAtIsNull(
                        farmZone.getFarmPlot().getId(), request.getZoneName())) {
            throw new BadRequestException("Zone name already exists in this farm plot");
        }

        if (request.getZoneName() != null) farmZone.setZoneName(request.getZoneName());
        if (request.getZoneCode() != null) farmZone.setZoneCode(request.getZoneCode());
        if (request.getDescription() != null) farmZone.setDescription(request.getDescription());
        if (request.getAreaM2() != null) farmZone.setAreaM2(request.getAreaM2());
        if (request.getSoilType() != null) farmZone.setSoilType(request.getSoilType());
        if (request.getCropType() != null) farmZone.setCropType(request.getCropType());
        if (request.getPlantingDate() != null) farmZone.setPlantingDate(request.getPlantingDate());
        if (request.getElevationM() != null) farmZone.setElevationM(request.getElevationM());
        if (request.getBoundaryGeojson() != null) farmZone.setBoundaryGeojson(request.getBoundaryGeojson());
        if (request.getStatus() != null) farmZone.setStatus(request.getStatus());

        return FarmMapper.toFarmZoneResponse(farmZoneRepository.save(farmZone));
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        FarmZone farmZone = getActiveFarmZone(id);
        farmZone.setDeletedAt(Instant.now());
        farmZoneRepository.save(farmZone);
    }

    private FarmPlot getActiveFarmPlot(UUID id) {
        return farmPlotRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm plot not found: " + id));
    }

    private FarmZone getActiveFarmZone(UUID id) {
        return farmZoneRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm zone not found: " + id));
    }

    private void validateCreateRequest(CreateFarmZoneRequest request) {
        if (request.getZoneName() == null || request.getZoneName().isBlank()) {
            throw new BadRequestException("zoneName is required");
        }
    }
}