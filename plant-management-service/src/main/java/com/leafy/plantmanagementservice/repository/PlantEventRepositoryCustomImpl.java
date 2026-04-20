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
        if (StringUtils.hasText(farmPlotId)) {
            criteriaList.add(Criteria.where("farmPlotId").is(farmPlotId.trim()));
        }
        if (StringUtils.hasText(farmZoneId)) {
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
}
