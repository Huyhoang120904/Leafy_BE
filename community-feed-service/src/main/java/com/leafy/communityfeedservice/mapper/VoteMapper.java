package com.leafy.communityfeedservice.mapper;

import com.leafy.communityfeedservice.dto.response.VoteResponse;
import com.leafy.communityfeedservice.model.Vote;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VoteMapper {
    VoteResponse toResponse(Vote vote);
}