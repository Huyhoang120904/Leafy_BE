package com.leafy.farmservice.repository.custom;

import com.leafy.farmservice.model.FarmZone;
import com.leafy.farmservice.model.enums.FarmZoneStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FarmZoneRepositoryCustom {

    Page<FarmZone> findZonesFiltered(
            String searchTerm,
            FarmZoneStatus status,
            String cropType,
            String soilType,
            Double minAreaM2,
            Double maxAreaM2,
            Pageable pageable);
}
