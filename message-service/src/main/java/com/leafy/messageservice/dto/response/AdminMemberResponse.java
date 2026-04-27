package com.leafy.messageservice.dto.response;

import com.leafy.messageservice.model.enums.MemberRole;
import lombok.Builder;

@Builder
public record AdminMemberResponse(
        String userId,
        String fullName,
        String avatar,
        MemberRole role
) {}
