package com.leafy.userservice.controller;

import com.leafy.common.dto.ApiResponse;
import com.leafy.userservice.dto.request.ProfileCreateRequest;
import com.leafy.userservice.dto.request.ProfileUpdateRequest;
import com.leafy.userservice.dto.response.ProfileDetailsResponse;
import com.leafy.userservice.dto.response.ProfileResponse;
import com.leafy.userservice.service.profile.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Profile management
 * Provides endpoints for CRUD operations on user profiles
 */
@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Create a new profile
     *
     * @param request the profile create request
     * @return the created profile response
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ProfileResponse>> createProfile(@Valid @RequestBody ProfileCreateRequest request) {
        log.info("POST /profiles - Creating new profile for user ID: {}", request.getUserId());
        ProfileResponse response = profileService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * Update an existing profile by profile ID
     *
     * @param profileId the profile ID
     * @param request   the profile update request
     * @return the updated profile response
     */
    @PutMapping("/{profileId}")
    @PreAuthorize("hasAuthority('ADMIN') or @profileSecurityService.isOwner(#profileId)")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @PathVariable String profileId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        log.info("PUT /profiles/{} - Updating profile", profileId);
        ProfileResponse response = profileService.updateProfile(profileId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update profile by user ID
     *
     * @param userId  the user ID
     * @param request the profile update request
     * @return the updated profile response
     */
    @PutMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('ADMIN') or @userSecurityService.isCurrentUser(#userId)")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfileByUserId(
            @PathVariable String userId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        log.info("PUT /profiles/user/{} - Updating profile by user ID", userId);
        ProfileResponse response = profileService.updateProfileByUserId(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get profile by profile ID
     *
     * @param profileId the profile ID
     * @return the profile response
     */
    @GetMapping("/{profileId}")
    @PreAuthorize("hasAuthority('ADMIN') or @profileSecurityService.isOwner(#profileId)")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfileById(@PathVariable String profileId) {
        log.info("GET /profiles/{} - Getting profile by ID", profileId);
        ProfileResponse response = profileService.getProfileById(profileId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get profile details by profile ID (includes audit fields)
     *
     * @param profileId the profile ID
     * @return the profile details response
     */
    @GetMapping("/{profileId}/details")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ProfileDetailsResponse>> getProfileDetailsById(@PathVariable String profileId) {
        log.info("GET /profiles/{}/details - Getting profile details by ID", profileId);
        ProfileDetailsResponse response = profileService.getProfileDetailsById(profileId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get profile by user ID
     *
     * @param userId the user ID
     * @return the profile response
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('ADMIN') or @userSecurityService.isCurrentUser(#userId)")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfileByUserId(@PathVariable String userId) {
        log.info("GET /profiles/user/{} - Getting profile by user ID", userId);
        ProfileResponse response = profileService.getProfileByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get profile by email
     *
     * @param email the email
     * @return the profile response
     */
    @GetMapping("/email/{email}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfileByEmail(@PathVariable String email) {
        log.info("GET /profiles/email/{} - Getting profile by email", email);
        ProfileResponse response = profileService.getProfileByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get profile by phone number
     *
     * @param phoneNumber the phone number
     * @return the profile response
     */
    @GetMapping("/phone/{phoneNumber}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfileByPhoneNumber(@PathVariable String phoneNumber) {
        log.info("GET /profiles/phone/{} - Getting profile by phone number", phoneNumber);
        ProfileResponse response = profileService.getProfileByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all profiles with pagination and sorting
     *
     * @param page    page number (default: 0)
     * @param size    page size (default: 20)
     * @param sortBy  field to sort by (default: createdAt)
     * @param sortDir sort direction (default: DESC)
     * @return page of profile responses
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProfileResponse>>> getAllProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        log.info("GET /profiles - Getting all profiles with pagination");

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProfileResponse> response = profileService.getAllProfiles(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all active profiles with pagination
     *
     * @param page    page number (default: 0)
     * @param size    page size (default: 20)
     * @param sortBy  field to sort by (default: createdAt)
     * @param sortDir sort direction (default: DESC)
     * @return page of active profile responses
     */
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProfileResponse>>> getActiveProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        log.info("GET /profiles/active - Getting all active profiles with pagination");

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProfileResponse> response = profileService.getActiveProfiles(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Search profiles by search term
     *
     * @param searchTerm search term
     * @param page       page number (default: 0)
     * @param size       page size (default: 20)
     * @param sortBy     field to sort by (default: createdAt)
     * @param sortDir    sort direction (default: DESC)
     * @return page of matching profile responses
     */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProfileResponse>>> searchProfiles(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        log.info("GET /profiles/search - Searching profiles with term: {}", searchTerm);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProfileResponse> response = profileService.searchProfiles(searchTerm, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete profile by profile ID
     *
     * @param profileId the profile ID
     * @return success response
     */
    @DeleteMapping("/{profileId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(@PathVariable String profileId) {
        log.info("DELETE /profiles/{} - Deleting profile", profileId);
        profileService.deleteProfile(profileId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Delete profile by user ID
     *
     * @param userId the user ID
     * @return success response
     */
    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProfileByUserId(@PathVariable String userId) {
        log.info("DELETE /profiles/user/{} - Deleting profile by user ID", userId);
        profileService.deleteProfileByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Activate a profile
     *
     * @param profileId the profile ID
     * @return the updated profile response
     */
    @PatchMapping("/{profileId}/activate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ProfileResponse>> activateProfile(@PathVariable String profileId) {
        log.info("PATCH /profiles/{}/activate - Activating profile", profileId);
        ProfileResponse response = profileService.activateProfile(profileId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Deactivate a profile
     *
     * @param profileId the profile ID
     * @return the updated profile response
     */
    @PatchMapping("/{profileId}/deactivate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ProfileResponse>> deactivateProfile(@PathVariable String profileId) {
        log.info("PATCH /profiles/{}/deactivate - Deactivating profile", profileId);
        ProfileResponse response = profileService.deactivateProfile(profileId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Check if profile exists for user ID
     *
     * @param userId the user ID
     * @return true if exists, false otherwise
     */
    @GetMapping("/exists/user/{userId}")
    @PreAuthorize("hasAuthority('ADMIN') or @userSecurityService.isCurrentUser(#userId)")
    public ResponseEntity<ApiResponse<Boolean>> existsByUserId(@PathVariable String userId) {
        log.info("GET /profiles/exists/user/{} - Checking if profile exists for user", userId);
        boolean exists = profileService.existsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }
}
