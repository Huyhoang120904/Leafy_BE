package com.leafy.communityfeedservice.controller;

import com.leafy.common.dto.ApiResponse;
import com.leafy.communityfeedservice.dto.response.SeederResponse;
import com.leafy.communityfeedservice.service.seeder.SeederService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/seed")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeederController {

    SeederService seederService;

    @PostMapping("/community")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SeederResponse> reseedCommunityFeed() {
        return ApiResponse.success(seederService.reseedCommunityFeed());
    }
}
