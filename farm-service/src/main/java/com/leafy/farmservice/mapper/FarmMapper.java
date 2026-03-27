package com.leafy.farmservice.mapper;

import com.leafy.farmservice.dto.response.FarmPlotResponse;
import com.leafy.farmservice.dto.response.FarmZoneResponse;
import com.leafy.farmservice.model.FarmPlot;
import com.leafy.farmservice.model.FarmZone;

public final class FarmMapper {

    private FarmMapper() {
    }

    public static FarmPlotResponse toFarmPlotResponse(FarmPlot farmPlot) {
        return FarmPlotResponse.builder()
                .id(farmPlot.getId())
                .ownerUserId(farmPlot.getOwnerUserId())
                .name(farmPlot.getName())
                .code(farmPlot.getCode())
                .description(farmPlot.getDescription())
                .areaM2(farmPlot.getAreaM2())
                .addressLine(farmPlot.getAddressLine())
                .provinceCode(farmPlot.getProvinceCode())
                .districtCode(farmPlot.getDistrictCode())
                .wardCode(farmPlot.getWardCode())
                .latitude(farmPlot.getLatitude())
                .longitude(farmPlot.getLongitude())
                .boundaryGeojson(farmPlot.getBoundaryGeojson())
                .status(farmPlot.getStatus())
                .createdAt(farmPlot.getCreatedAt())
                .updatedAt(farmPlot.getUpdatedAt())
                .build();
    }

    public static FarmZoneResponse toFarmZoneResponse(FarmZone farmZone) {
        return FarmZoneResponse.builder()
                .id(farmZone.getId())
                .farmPlotId(farmZone.getFarmPlot().getId())
                .zoneName(farmZone.getZoneName())
                .zoneCode(farmZone.getZoneCode())
                .description(farmZone.getDescription())
                .areaM2(farmZone.getAreaM2())
                .soilType(farmZone.getSoilType())
                .cropType(farmZone.getCropType())
                .plantingDate(farmZone.getPlantingDate())
                .elevationM(farmZone.getElevationM())
                .boundaryGeojson(farmZone.getBoundaryGeojson())
                .status(farmZone.getStatus())
                .createdAt(farmZone.getCreatedAt())
                .updatedAt(farmZone.getUpdatedAt())
                .build();
    }
}