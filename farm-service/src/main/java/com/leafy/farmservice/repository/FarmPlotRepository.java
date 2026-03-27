package com.leafy.farmservice.repository;

import com.leafy.farmservice.model.FarmPlot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmPlotRepository extends JpaRepository<FarmPlot, UUID> {

    List<FarmPlot> findByOwnerUserIdAndDeletedAtIsNull(UUID ownerUserId);

    Optional<FarmPlot> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByCode(String code);
}