package com.leafy.farmservice.service.impl;

import com.leafy.farmservice.dto.request.CreateFarmPlotRequest;
import com.leafy.farmservice.dto.request.UpdateFarmPlotRequest;
import com.leafy.farmservice.dto.response.FarmPlotResponse;
import com.leafy.farmservice.exception.BadRequestException;
import com.leafy.farmservice.exception.ResourceNotFoundException;
import com.leafy.farmservice.mapper.FarmMapper;
import com.leafy.farmservice.model.FarmPlot;
import com.leafy.farmservice.repository.FarmPlotRepository;
import com.leafy.farmservice.service.FarmPlotService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FarmPlotServiceImpl implements FarmPlotService {

    private final FarmPlotRepository farmPlotRepository;

    @Override
    @Transactional
    public FarmPlotResponse create(CreateFarmPlotRequest request) {
        validateCreateRequest(request);

        if (request.getCode() != null && !request.getCode().isBlank()
                && farmPlotRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Farm plot code already exists");
        }

        FarmPlot farmPlot = new FarmPlot();
        farmPlot.setOwnerUserId(request.getOwnerUserId());
        farmPlot.setName(request.getName());
        farmPlot.setCode(request.getCode());
        farmPlot.setDescription(request.getDescription());
        farmPlot.setAreaM2(request.getAreaM2());
        farmPlot.setAddressLine(request.getAddressLine());
        farmPlot.setProvinceCode(request.getProvinceCode());
        farmPlot.setDistrictCode(request.getDistrictCode());
        farmPlot.setWardCode(request.getWardCode());
        farmPlot.setLatitude(request.getLatitude());
        farmPlot.setLongitude(request.getLongitude());
        farmPlot.setBoundaryGeojson(request.getBoundaryGeojson());

        return FarmMapper.toFarmPlotResponse(farmPlotRepository.save(farmPlot));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmPlotResponse> getByOwner(UUID ownerUserId) {
        return farmPlotRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .stream()
                .map(FarmMapper::toFarmPlotResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FarmPlotResponse getById(UUID id) {
        FarmPlot farmPlot = getActiveFarmPlot(id);
        return FarmMapper.toFarmPlotResponse(farmPlot);
    }

    @Override
    @Transactional
    public FarmPlotResponse update(UUID id, UpdateFarmPlotRequest request) {
        FarmPlot farmPlot = getActiveFarmPlot(id);

        if (request.getCode() != null && !request.getCode().isBlank()
                && !request.getCode().equals(farmPlot.getCode())
                && farmPlotRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Farm plot code already exists");
        }

        if (request.getName() != null) farmPlot.setName(request.getName());
        if (request.getCode() != null) farmPlot.setCode(request.getCode());
        if (request.getDescription() != null) farmPlot.setDescription(request.getDescription());
        if (request.getAreaM2() != null) farmPlot.setAreaM2(request.getAreaM2());
        if (request.getAddressLine() != null) farmPlot.setAddressLine(request.getAddressLine());
        if (request.getProvinceCode() != null) farmPlot.setProvinceCode(request.getProvinceCode());
        if (request.getDistrictCode() != null) farmPlot.setDistrictCode(request.getDistrictCode());
        if (request.getWardCode() != null) farmPlot.setWardCode(request.getWardCode());
        if (request.getLatitude() != null) farmPlot.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) farmPlot.setLongitude(request.getLongitude());
        if (request.getBoundaryGeojson() != null) farmPlot.setBoundaryGeojson(request.getBoundaryGeojson());
        if (request.getStatus() != null) farmPlot.setStatus(request.getStatus());

        return FarmMapper.toFarmPlotResponse(farmPlotRepository.save(farmPlot));
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        FarmPlot farmPlot = getActiveFarmPlot(id);
        farmPlot.setDeletedAt(Instant.now());
        farmPlotRepository.save(farmPlot);
    }

    private FarmPlot getActiveFarmPlot(UUID id) {
        return farmPlotRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm plot not found: " + id));
    }

    private void validateCreateRequest(CreateFarmPlotRequest request) {
        if (request.getOwnerUserId() == null) {
            throw new BadRequestException("ownerUserId is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("name is required");
        }
    }
}