package com.leafy.userservice.service.security;

import com.leafy.common.exception.AppException;
import com.leafy.common.exception.ErrorCode;
import com.leafy.common.utils.ServiceSecurityUtils;
import com.leafy.userservice.model.Profile;
import com.leafy.userservice.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Security service for profile-related authorization checks
 */
@Service("profileSecurityService")
@RequiredArgsConstructor
@Slf4j
public class ProfileSecurityService {

    private final ProfileRepository profileRepository;

    /**
     * Check if the current authenticated user is the owner of the specified profile
     *
     * @param profileId the profile ID to check
     * @return true if current user owns the profile, false otherwise
     */
    public boolean isOwner(String profileId) {
        try {
            String currentUserId = ServiceSecurityUtils.getCurrentAccountId();
            
            Profile profile = profileRepository.findById(profileId)
                    .orElseThrow(() -> new AppException(ErrorCode.ACC_ACCOUNT_NOT_FOUND));
            
            boolean isOwner = currentUserId.equals(profile.getUserId());
            log.debug("Checking if current user ({}) owns profile ({}): {}",
                    currentUserId, profileId, isOwner);
            return isOwner;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error checking if current user owns profile: {}", e.getMessage());
            return false;
        }
    }
}
