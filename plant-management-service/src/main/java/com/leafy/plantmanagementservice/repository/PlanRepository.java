package com.leafy.plantmanagementservice.repository;

import com.leafy.plantmanagementservice.model.Plan;
import com.leafy.plantmanagementservice.model.enums.PlanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends MongoRepository<Plan, String> {

    Page<Plan> findByUserId(String userId, Pageable pageable);

    Page<Plan> findByPlantId(String plantId, Pageable pageable);

    Page<Plan> findByFarmPlotId(String farmPlotId, Pageable pageable);

    Page<Plan> findByFarmZoneId(String farmZoneId, Pageable pageable);

    Page<Plan> findByUserIdAndStatus(String userId, PlanStatus status, Pageable pageable);

    Page<Plan> findByStatus(PlanStatus status, Pageable pageable);

    Page<Plan> findByOwnerId(String ownerId, Pageable pageable);

    Page<Plan> findByOwnerIdAndStatus(String ownerId, PlanStatus status, Pageable pageable);

    Page<Plan> findByCreatorId(String creatorId, Pageable pageable);

    Page<Plan> findByOwnerIdOrCreatorId(String ownerId, String creatorId, Pageable pageable);

    Optional<Plan> findByRagPlanId(String ragPlanId);

    List<Plan> findByStatus(PlanStatus status);
}
