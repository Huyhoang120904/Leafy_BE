package com.leafy.plantmanagementservice.mapper;

import com.leafy.plantmanagementservice.dto.request.plan.PlanCreateRequest;
import com.leafy.plantmanagementservice.dto.response.plan.PlanResponse;
import com.leafy.plantmanagementservice.model.Plan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PlanMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "plantEventIds", ignore = true)
    @Mapping(target = "status", ignore = true)
    Plan toEntity(PlanCreateRequest request);

    @Mapping(target = "isPublic",    expression = "java(plan.isPublic())")
    @Mapping(target = "isConsulted", expression = "java(plan.isConsulted())")
    PlanResponse toResponse(Plan plan);

    @Mapping(target = "isPublic",    expression = "java(plan.isPublic())")
    @Mapping(target = "isConsulted", expression = "java(plan.isConsulted())")
    List<PlanResponse> toResponseList(List<Plan> plans);

}
