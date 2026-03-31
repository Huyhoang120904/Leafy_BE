package com.leafy.authservice.controller.internal;

import com.leafy.authservice.dto.response.UserProfileSeederResponse;
import com.leafy.authservice.dto.response.UserResponse;
import com.leafy.authservice.service.seeder.UserProfileSeederService;
import com.leafy.authservice.service.user.UserService;
import com.leafy.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal endpoints for service-to-service user lookup.
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Slf4j
public class InternalUserController {

    private final UserService userService;
    private final UserProfileSeederService userProfileSeederService;

    @GetMapping("/seed")
    public ResponseEntity<ApiResponse<UserProfileSeederResponse>> seedUsersAndProfilesViaGet(
            @RequestParam(name = "q", defaultValue = "10") int quantity) {
        log.info("GET /internal/users/seed - Seeding users and profiles. Quantity: {}", quantity);
        UserProfileSeederResponse response = userProfileSeederService.seedUsersAndProfiles(quantity);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/seed")
    public ResponseEntity<ApiResponse<UserProfileSeederResponse>> seedUsersAndProfiles(
            @RequestParam(name = "q", defaultValue = "10") int quantity) {
        log.info("POST /internal/users/seed - Seeding users and profiles. Quantity: {}", quantity);
        UserProfileSeederResponse response = userProfileSeederService.seedUsersAndProfiles(quantity);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


}
