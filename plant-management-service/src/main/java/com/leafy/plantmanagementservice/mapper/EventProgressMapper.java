package com.leafy.plantmanagementservice.mapper;

import com.leafy.plantmanagementservice.dto.response.plantevent.EventProgressResponse;
import com.leafy.plantmanagementservice.model.EventProgress;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EventProgressMapper {

    EventProgressResponse toResponse(EventProgress entity);

    List<EventProgressResponse> toResponseList(List<EventProgress> entities);
}
