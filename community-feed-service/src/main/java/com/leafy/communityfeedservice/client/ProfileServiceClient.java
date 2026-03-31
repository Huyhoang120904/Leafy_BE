package com.leafy.communityfeedservice.client;

import com.leafy.communityfeedservice.client.dto.ExternalApiResponse;
import com.leafy.communityfeedservice.client.dto.PagedResponse;
import com.leafy.communityfeedservice.client.dto.ProfileSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "profile-service")
public interface ProfileServiceClient {

    @GetMapping("/profiles/active")
    ExternalApiResponse<PagedResponse<ProfileSummaryResponse>> getActiveProfiles(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sortBy") String sortBy,
            @RequestParam("sortDir") String sortDir,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Roles") String userRoles,
            @RequestHeader("X-Profile-Id") String profileId);
}
