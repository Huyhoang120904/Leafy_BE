package com.leafy.profileservice.controller;

import com.leafy.common.dto.ApiResponse;
import com.leafy.profileservice.dto.request.profile.InternalCreateProfileRequest;
import com.leafy.profileservice.dto.response.profile.ProfileResponse;
import com.leafy.profileservice.service.profile.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal REST Controller for Profile management
 * Handles service-to-service profile operations, not exposed through the API gateway
 */
@RestController
@RequestMapping("/internal/profiles")
@RequiredArgsConstructor
@Slf4j
public class InternalProfileController {

    private final ProfileService profileService;

    /**
     * Create a minimal profile for a newly registered user
     * Called internally by auth-service after successful user registration
     *
     * @param request the internal create profile request containing the user ID
     * @return the created profile response
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> createProfile(
            @Valid @RequestBody InternalCreateProfileRequest request) {
        log.info("POST /internal/profiles - Creating profile for user: {}", request.getUserId());
        ProfileResponse response = profileService.createProfileInternal(request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}
