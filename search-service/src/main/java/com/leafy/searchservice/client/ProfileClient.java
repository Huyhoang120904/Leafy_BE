package com.leafy.searchservice.client;

import com.leafy.common.dto.ApiResponse;
import com.leafy.searchservice.client.dto.ProfileServiceProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "profile-service", path = "/internal/profiles")
public interface ProfileClient {

    @GetMapping("/{profileId}")
    ApiResponse<ProfileServiceProfileResponse> getProfileById(@PathVariable("profileId") String profileId);
}
