package com.leafy.plantmanagementservice.repository;

import com.leafy.plantmanagementservice.model.Incident;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends MongoRepository<Incident, String> {

    Optional<Incident> findByPlanApplyId(String planApplyId);

    List<Incident> findByPlantId(String plantId);

    List<Incident> findByFarmZoneId(String farmZoneId);

    List<Incident> findByFarmPlotId(String farmPlotId);
}
