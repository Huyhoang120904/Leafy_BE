package com.leafy.profileservice.controller.internal;

import com.leafy.common.dto.ApiResponse;
import com.leafy.profileservice.service.connection.UserConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/profiles/consulting")
@RequiredArgsConstructor
public class ConsultingInternalController {

    private final UserConnectionService userConnectionService;

    /**
     * Validate that an expert actively consults a given farmer.
     *
     * @param expertProfileId  the profile ID of the expert
     * @param farmerProfileId  the profile ID of the farmer
     * @return true if an ACCEPTED consultation relationship exists, false otherwise
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateConsulting(
            @RequestParam String expertProfileId,
            @RequestParam String farmerProfileId) {
        boolean valid = userConnectionService.isActiveConsultation(expertProfileId, farmerProfileId);
        return ResponseEntity.ok(ApiResponse.success(valid));
    }

    /**
     * Get all farmer profile IDs that the given expert is actively consulting.
     *
     * @param expertProfileId  the profile ID of the expert
     * @return list of farmer profile IDs with ACCEPTED consultation status
     */
    @GetMapping("/farmers")
    public ResponseEntity<ApiResponse<List<String>>> getConsultingFarmers(
            @RequestParam String expertProfileId) {
        List<String> farmerIds = userConnectionService.getConsultingFarmerIds(expertProfileId);
        return ResponseEntity.ok(ApiResponse.success(farmerIds));
    }
}
