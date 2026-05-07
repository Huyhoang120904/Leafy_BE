package com.leafy.plantmanagementservice.helper;

import com.leafy.common.exception.AppException;
import com.leafy.common.exception.ErrorCode;
import com.leafy.plantmanagementservice.client.ProfileServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Helper component that encapsulates consulting access validation.
 * Calls profile-service internally to verify the expert-farmer consultation relationship.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConsultingAccessHelper {

    private final ProfileServiceClient profileServiceClient;

    /**
     * Validates that the given expert actively consults the given farmer.
     * Throws {@link AppException} with {@code AUTH_UNAUTHORIZED} if the relationship
     * does not exist, is not ACCEPTED, or if the validation call itself fails.
     */
    public void requireConsultingAccess(String expertProfileId, String farmerProfileId) {
        try {
            Boolean valid = profileServiceClient.validateConsulting(expertProfileId, farmerProfileId).getData();
            if (!Boolean.TRUE.equals(valid)) {
                throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Consulting validation failed for expert={}, farmer={}: {}", expertProfileId, farmerProfileId, e.getMessage());
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }
    }

    /**
     * Validates that the given expert actively consults ALL of the provided farmers.
     * Uses a single call to {@code /internal/profiles/consulting/farmers} instead of
     * one {@code /validate} call per farmer, making it O(1) network hops.
     */
    public void requireBulkConsultingAccess(String expertProfileId, List<String> farmerProfileIds) {
        if (farmerProfileIds == null || farmerProfileIds.isEmpty()) {
            return;
        }
        try {
            List<String> allowedFarmerIds = profileServiceClient.getConsultingFarmerIds(expertProfileId).getData();
            Set<String> allowedSet = allowedFarmerIds != null ? new java.util.HashSet<>(allowedFarmerIds) : java.util.Collections.emptySet();
            for (String farmerId : farmerProfileIds) {
                if (!allowedSet.contains(farmerId)) {
                    log.warn("Bulk consulting access denied for expert={}, farmer={}", expertProfileId, farmerId);
                    throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
                }
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Bulk consulting validation failed for expert={}: {}", expertProfileId, e.getMessage());
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }
    }
}
