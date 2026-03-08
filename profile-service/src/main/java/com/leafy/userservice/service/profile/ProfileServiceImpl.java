package com.leafy.userservice.service.profile;

import com.leafy.common.exception.AppException;
import com.leafy.common.exception.ErrorCode;
import com.leafy.userservice.dto.request.ProfileCreateRequest;
import com.leafy.userservice.dto.request.ProfileUpdateRequest;
import com.leafy.userservice.dto.response.ProfileDetailsResponse;
import com.leafy.userservice.dto.response.ProfileResponse;
import com.leafy.userservice.mapper.ProfileMapper;
import com.leafy.userservice.model.Profile;
import com.leafy.userservice.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ProfileService
 * Handles all business logic for profile management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;

    @Override
    public ProfileResponse createProfile(ProfileCreateRequest request) {
        log.info("Creating new profile for user ID: {}", request.getUserId());

        // Check if profile already exists for this user
        if (profileRepository.existsByUserId(request.getUserId())) {
            log.error("Profile already exists for user ID: {}", request.getUserId());
            throw new AppException(ErrorCode.ACC_ACCOUNT_NOT_FOUND); // TODO: Add proper error code for profile exists
        }

        // Check if email is already in use
        if (request.getEmail() != null && profileRepository.existsByEmail(request.getEmail())) {
            log.error("Email already in use: {}", request.getEmail());
            throw new AppException(ErrorCode.ACC_EMAIL_ALREADY_USED);
        }

        // Check if phone number is already in use
        if (request.getPhoneNumber() != null && profileRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            log.error("Phone number already in use: {}", request.getPhoneNumber());
            throw new AppException(ErrorCode.ACC_PHONE_NUMBER_ALREADY_USED);
        }

        Profile profile = profileMapper.toEntity(request);
        profile.setActive(true);

        Profile savedProfile = profileRepository.save(profile);
        log.info("Profile created successfully with ID: {}", savedProfile.getId());

        return profileMapper.toResponse(savedProfile);
    }

    @Override
    public ProfileResponse updateProfile(String profileId, ProfileUpdateRequest request) {
        log.info("Updating profile with ID: {}", profileId);

        Profile profile = getProfileEntityById(profileId);

        // Check email uniqueness (if being updated)
        if (request.getEmail() != null && !request.getEmail().equals(profile.getEmail())) {
            if (profileRepository.existsByEmail(request.getEmail())) {
                log.error("Email already in use: {}", request.getEmail());
                throw new AppException(ErrorCode.ACC_EMAIL_ALREADY_USED);
            }
        }

        // Check phone number uniqueness (if being updated)
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(profile.getPhoneNumber())) {
            if (profileRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                log.error("Phone number already in use: {}", request.getPhoneNumber());
                throw new AppException(ErrorCode.ACC_PHONE_NUMBER_ALREADY_USED);
            }
        }

        profileMapper.updateEntityFromRequest(request, profile);
        Profile updatedProfile = profileRepository.save(profile);

        log.info("Profile updated successfully with ID: {}", updatedProfile.getId());
        return profileMapper.toResponse(updatedProfile);
    }

    @Override
    public ProfileResponse updateProfileByUserId(String userId, ProfileUpdateRequest request) {
        log.info("Updating profile by user ID: {}", userId);

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("Profile not found for user ID: {}", userId);
                    return new AppException(ErrorCode.ACC_ACCOUNT_NOT_FOUND);
                });

        return updateProfile(profile.getId(), request);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(String profileId) {
        log.info("Getting profile by ID: {}", profileId);
        Profile profile = getProfileEntityById(profileId);
        return profileMapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileDetailsResponse getProfileDetailsById(String profileId) {
        log.info("Getting profile details by ID: {}", profileId);
        Profile profile = getProfileEntityById(profileId);
        return profileMapper.toDetailsResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Profile getProfileEntityById(String profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> {
                    log.error("Profile not found with ID: {}", profileId);
                    return new AppException(ErrorCode.ACC_ACCOUNT_NOT_FOUND);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfileByUserId(String userId) {
        log.info("Getting profile by user ID: {}", userId);
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("Profile not found for user ID: {}", userId);
                    return new AppException(ErrorCode.ACC_ACCOUNT_NOT_FOUND);
                });
        return profileMapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfileByEmail(String email) {
        log.info("Getting profile by email: {}", email);
        Profile profile = profileRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Profile not found with email: {}", email);
                    return new AppException(ErrorCode.ACC_ACCOUNT_NOT_FOUND);
                });
        return profileMapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfileByPhoneNumber(String phoneNumber) {
        log.info("Getting profile by phone number: {}", phoneNumber);
        Profile profile = profileRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> {
                    log.error("Profile not found with phone number: {}", phoneNumber);
                    return new AppException(ErrorCode.ACC_ACCOUNT_NOT_FOUND);
                });
        return profileMapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProfileResponse> getAllProfiles(Pageable pageable) {
        log.info("Getting all profiles with pagination");
        Page<Profile> profiles = profileRepository.findAll(pageable);
        return profiles.map(profileMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProfileResponse> getActiveProfiles(Pageable pageable) {
        log.info("Getting all active profiles with pagination");
        Page<Profile> profiles = profileRepository.findByActiveTrue(pageable);
        return profiles.map(profileMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProfileResponse> searchProfiles(String searchTerm, Pageable pageable) {
        log.info("Searching profiles with term: {}", searchTerm);
        Page<Profile> profiles = profileRepository.searchProfiles(searchTerm, pageable);
        return profiles.map(profileMapper::toResponse);
    }

    @Override
    public void deleteProfile(String profileId) {
        log.info("Deleting profile with ID: {}", profileId);
        Profile profile = getProfileEntityById(profileId);
        profileRepository.delete(profile);
        log.info("Profile deleted successfully with ID: {}", profileId);
    }

    @Override
    public void deleteProfileByUserId(String userId) {
        log.info("Deleting profile by user ID: {}", userId);
        profileRepository.deleteByUserId(userId);
        log.info("Profile deleted successfully for user ID: {}", userId);
    }

    @Override
    public ProfileResponse activateProfile(String profileId) {
        log.info("Activating profile with ID: {}", profileId);
        Profile profile = getProfileEntityById(profileId);
        profile.setActive(true);
        Profile activatedProfile = profileRepository.save(profile);
        log.info("Profile activated successfully with ID: {}", profileId);
        return profileMapper.toResponse(activatedProfile);
    }

    @Override
    public ProfileResponse deactivateProfile(String profileId) {
        log.info("Deactivating profile with ID: {}", profileId);
        Profile profile = getProfileEntityById(profileId);
        profile.setActive(false);
        Profile deactivatedProfile = profileRepository.save(profile);
        log.info("Profile deactivated successfully with ID: {}", profileId);
        return profileMapper.toResponse(deactivatedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserId(String userId) {
        return profileRepository.existsByUserId(userId);
    }
}
