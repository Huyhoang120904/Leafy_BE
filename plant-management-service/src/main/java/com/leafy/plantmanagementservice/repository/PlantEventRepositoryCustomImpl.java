package com.leafy.plantmanagementservice.repository;

import com.leafy.plantmanagementservice.model.PlantEvent;
import com.leafy.plantmanagementservice.model.enums.EventType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PlantEventRepositoryCustomImpl implements PlantEventRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    // ── Admin multi-criteria filter ───────────────────────────────────────────

    @Override
    public Page<PlantEvent> findAllByFilters(
            EventType eventType,
            Boolean planned,
            String farmPlotId,
            String farmZoneId,
            Pageable pageable
    ) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (eventType != null) {
            criteriaList.add(Criteria.where("eventType").is(eventType));
        }
        if (planned != null) {
            criteriaList.add(Criteria.where("planned").is(planned));
        }

        // farmPlotId and farmZoneId are OR'd — an event is attached to one or the other
        boolean hasPlotFilter = StringUtils.hasText(farmPlotId);
        boolean hasZoneFilter = StringUtils.hasText(farmZoneId);
        if (hasPlotFilter && hasZoneFilter) {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("farmPlotId").is(farmPlotId.trim()),
                    Criteria.where("farmZoneId").is(farmZoneId.trim())
            ));
        } else if (hasPlotFilter) {
            criteriaList.add(Criteria.where("farmPlotId").is(farmPlotId.trim()));
        } else if (hasZoneFilter) {
            criteriaList.add(Criteria.where("farmZoneId").is(farmZoneId.trim()));
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, PlantEvent.class);
        query.with(pageable);
        List<PlantEvent> results = mongoTemplate.find(query, PlantEvent.class);
        return new PageImpl<>(results, pageable, total);
    }

    // ── Shared date-overlap criteria ─────────────────────────────────────────
    //
    // An event overlaps [startDate, endDate] when:
    //   calculatedStartDate <= endDate  AND  calculatedEndDate >= startDate
    //
    // Events with null calculatedEndDate are treated as single-day events:
    // they are included when calculatedStartDate falls inside [startDate, endDate].
    //
    private Criteria buildDateOverlapCriteria(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        // Events that have an explicit end date and overlap the range
        Criteria rangedEvent = new Criteria().andOperator(
                Criteria.where("calculatedStartDate").ne(null).lte(endDate),
                Criteria.where("calculatedEndDate").ne(null).gte(startDate)
        );
        // Single-day events (no end date) whose start date falls inside the range
        Criteria singleDayEvent = new Criteria().andOperator(
                Criteria.where("calculatedStartDate").gte(startDate).lte(endDate),
                new Criteria().orOperator(
                        Criteria.where("calculatedEndDate").exists(false),
                        Criteria.where("calculatedEndDate").is(null)
                )
        );
        return new Criteria().orOperator(rangedEvent, singleDayEvent);
    }

    // ── Profile calendar events (broad ownership scope) ───────────────────────

    @Override
    public List<PlantEvent> findProfileCalendarEvents(
            List<String> farmPlotIds,
            List<String> farmZoneIds,
            List<String> plantIds,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    ) {
        Criteria dateCriteria = buildDateOverlapCriteria(startDate, endDate);

        List<Criteria> orCriterias = new ArrayList<>();
        if (farmPlotIds != null && !farmPlotIds.isEmpty()) {
            orCriterias.add(Criteria.where("farmPlotId").in(farmPlotIds));
        }
        if (farmZoneIds != null && !farmZoneIds.isEmpty()) {
            orCriterias.add(Criteria.where("farmZoneId").in(farmZoneIds));
        }
        if (plantIds != null && !plantIds.isEmpty()) {
            orCriterias.add(Criteria.where("plantId").in(plantIds));
        }

        if (orCriterias.isEmpty()) {
            return new ArrayList<>();
        }

        Criteria targetCriteria = new Criteria().orOperator(orCriterias.toArray(new Criteria[0]));
        Query query = new Query(new Criteria().andOperator(dateCriteria, targetCriteria));
        return mongoTemplate.find(query, PlantEvent.class);
    }

    // ── Calendar events by specific plot OR zone (both filters given) ─────────

    @Override
    public List<PlantEvent> findByPlotOrZoneAndDateRange(
            String farmPlotId,
            String farmZoneId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    ) {
        Criteria dateCriteria = buildDateOverlapCriteria(startDate, endDate);
        Criteria locationCriteria = new Criteria().orOperator(
                Criteria.where("farmPlotId").is(farmPlotId.trim()),
                Criteria.where("farmZoneId").is(farmZoneId.trim())
        );
        Query query = new Query(new Criteria().andOperator(dateCriteria, locationCriteria));
        return mongoTemplate.find(query, PlantEvent.class);
    }

    // ── Entity-scoped calendar helpers ────────────────────────────────────────

    @Override
    public List<PlantEvent> findByPlantIdAndDateRange(
            String plantId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    ) {
        Criteria criteria = new Criteria().andOperator(
                buildDateOverlapCriteria(startDate, endDate),
                Criteria.where("plantId").is(plantId)
        );
        return mongoTemplate.find(new Query(criteria), PlantEvent.class);
    }

    @Override
    public List<PlantEvent> findByFarmPlotIdAndDateRange(
            String farmPlotId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    ) {
        Criteria criteria = new Criteria().andOperator(
                buildDateOverlapCriteria(startDate, endDate),
                Criteria.where("farmPlotId").is(farmPlotId)
        );
        return mongoTemplate.find(new Query(criteria), PlantEvent.class);
    }

    @Override
    public List<PlantEvent> findByFarmZoneIdAndDateRange(
            String farmZoneId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    ) {
        Criteria criteria = new Criteria().andOperator(
                buildDateOverlapCriteria(startDate, endDate),
                Criteria.where("farmZoneId").is(farmZoneId)
        );
        return mongoTemplate.find(new Query(criteria), PlantEvent.class);
    }

    @Override
    public List<PlantEvent> findBySourcePlanIdAndDateRange(
            String sourcePlanId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    ) {
        Criteria criteria = new Criteria().andOperator(
                buildDateOverlapCriteria(startDate, endDate),
                Criteria.where("sourcePlanId").is(sourcePlanId)
        );
        return mongoTemplate.find(new Query(criteria), PlantEvent.class);
    }
}
