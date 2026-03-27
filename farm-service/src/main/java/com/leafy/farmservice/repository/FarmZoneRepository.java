package com.leafy.farmservice.repository;

import com.leafy.farmservice.model.FarmZone;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmZoneRepository extends JpaRepository<FarmZone, UUID> {

    List<FarmZone> findByFarmPlotIdAndDeletedAtIsNull(UUID farmPlotId);

    Optional<FarmZone> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByFarmPlotIdAndZoneNameAndDeletedAtIsNull(UUID farmPlotId, String zoneName);
}