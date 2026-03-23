package com.leafy.profileservice.service.profile;

import com.leafy.common.exception.AppException;
import com.leafy.common.exception.ErrorCode;
import com.leafy.common.event.profile.ProfileUpsertEvent;
import com.leafy.common.model.kafka.EventType;
import com.leafy.common.publisher.OutboxEventPublisher;
import com.leafy.profileservice.dto.request.profile.ProfileCreateRequest;
import com.leafy.profileservice.dto.request.profile.InternalCreateProfileRequest;
import com.leafy.profileservice.dto.request.profile.ProfileUpdateRequest;
import com.leafy.profileservice.dto.response.profile.ProfileDetailsResponse;
import com.leafy.profileservice.dto.response.profile.ProfileResponse;
import com.leafy.profileservice.dto.response.profile.UserSyncResponse;
import com.leafy.profileservice.mapper.CertificateMapper;
import com.leafy.profileservice.mapper.ProfileMapper;
import com.leafy.profileservice.model.Certificate;
import com.leafy.profileservice.model.Profile;
import com.leafy.profileservice.model.enums.CertificateStatus;
import com.leafy.profileservice.repository.ApprovalRequestRepository;
import com.leafy.profileservice.repository.ProfileRepository;
import com.leafy.profileservice.client.AuthClient;
import com.leafy.profileservice.client.dto.UserResponse;
import com.leafy.common.dto.ApiResponse;
import com.leafy.common.enums.ProfileRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ProfileMapper profileMapper;
    private final CertificateMapper certificateMapper;
    private final AuthClient authClient;
    private final Optional<OutboxEventPublisher> outboxEventPublisher;

    @Override
    public ProfileResponse createProfile(ProfileCreateRequest request) {
        log.info("Creating new profile for user ID: {}", request.getUserId());

        if (profileRepository.existsByUserId(request.getUserId())) {
            log.error("Profile already exists for user ID: {}", request.getUserId());
            throw new AppException(ErrorCode.ACC_ACCOUNT_NOT_FOUND); // TODO: Add proper error code for profile exists
        }

        Profile profile = profileMapper.toEntity(request);
        profile.setActive(true);

        Profile savedProfile = profileRepository.save(profile);
        log.info("Profile created successfully with ID: {}", savedProfile.getId());

        ProfileResponse response = profileMapper.toResponse(savedProfile);
        ProfileResponse enrichedResponse = enrichWithUserInfo(response, savedProfile.getUserId());
        publishProfileUpsertEvent(savedProfile);
        return enrichedResponse;
    }

    @Override
    public ProfileResponse createProfileInternal(InternalCreateProfileRequest request) {
        String userId = request.getUserId();
        log.info("Creating minimal profile for new user: {}", userId);

        if (profileRepository.existsByUserId(userId)) {
            log.warn("Profile already exists for user ID: {}", userId);
            throw new AppException(ErrorCode.ACC_ACCOUNT_NOT_FOUND);
        }

        Profile profile = Profile.builder()
                .userId(userId)
                .fullName(request.getFullName())
                .role(ProfileRole.FARMER)
                .isVerified(false)
                .build();
        profile.setActive(true);

        Profile savedProfile = profileRepository.save(profile);
        log.info("Minimal profile created successfully for user: {}", userId);

        publishProfileUpsertEvent(savedProfile);

        return profileMapper.toResponse(savedProfile);
    }

    @Override
    public ProfileResponse updateProfile(String profileId, ProfileUpdateRequest request) {
        log.info("Updating profile with ID: {}", profileId);

        Profile profile = getProfileEntityById(profileId);

        profileMapper.updateEntityFromRequest(request, profile);
        Profile updatedProfile = profileRepository.save(profile);

        log.info("Profile updated successfully with ID: {}", updatedProfile.getId());
        ProfileResponse response = profileMapper.toResponse(updatedProfile);
        ProfileResponse enrichedResponse = enrichWithUserInfo(response, updatedProfile.getUserId());
        publishProfileUpsertEvent(updatedProfile);
        return enrichedResponse;
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
        return buildFullProfileResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileDetailsResponse getProfileDetailsById(String profileId) {
        log.info("Getting profile details by ID: {}", profileId);
        Profile profile = getProfileEntityById(profileId);
        ProfileDetailsResponse response = profileMapper.toDetailsResponse(profile);
        enrichProfileDetailsResponse(response, profile);
        return response;
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
        return buildFullProfileResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProfileResponse> getAllProfiles(Pageable pageable) {
        log.info("Getting all profiles with pagination");
        Page<Profile> profiles = profileRepository.findAll(pageable);
        return profiles.map(this::buildFullProfileResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProfileResponse> getActiveProfiles(Pageable pageable) {
        log.info("Getting all active profiles with pagination");
        Page<Profile> profiles = profileRepository.findByActiveTrue(pageable);
        return profiles.map(this::buildFullProfileResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProfileResponse> searchProfiles(String searchTerm, Pageable pageable) {
        log.info("Searching profiles with term: {}", searchTerm);
        Page<Profile> profiles = profileRepository.searchProfiles(searchTerm, pageable);
        return profiles.map(this::buildFullProfileResponse);
    }

    @Override
    public void deleteProfile(String profileId) {
        log.info("Deleting profile with ID: {}", profileId);
        Profile profile = getProfileEntityById(profileId);
        profileRepository.delete(profile);
        // 5. Delete associated approval requests
        approvalRequestRepository.deleteByProfileId(profileId);
        log.info("Profile deleted successfully with ID: {}", profileId);
    }

    @Override
    public void deleteProfileByUserId(String userId) {
        log.info("Deleting profile for user ID: {}", userId);
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.ACC_ACCOUNT_NOT_FOUND));

        profileRepository.delete(profile);

        // Delete associated approval requests
        approvalRequestRepository.deleteByProfileId(profile.getId());
        log.info("Profile deleted successfully for user ID: {}", userId);
    }

    @Override
    public ProfileResponse activateProfile(String profileId) {
        log.info("Activating profile with ID: {}", profileId);
        Profile profile = getProfileEntityById(profileId);
        profile.setActive(true);
        Profile activatedProfile = profileRepository.save(profile);
        log.info("Profile activated successfully with ID: {}", profileId);
        return buildFullProfileResponse(activatedProfile);
    }

    @Override
    public ProfileResponse deactivateProfile(String profileId) {
        log.info("Deactivating profile with ID: {}", profileId);
        Profile profile = getProfileEntityById(profileId);
        profile.setActive(false);
        Profile deactivatedProfile = profileRepository.save(profile);
        log.info("Profile deactivated successfully with ID: {}", profileId);
        return buildFullProfileResponse(deactivatedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserId(String userId) {
        return profileRepository.existsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSyncResponse> getUsersBatch(String lastId, int size) {
        int batchSize = size > 0 ? size : 500;
        Pageable pageable = PageRequest.of(0, batchSize);

        List<Profile> profiles = (lastId == null || lastId.isBlank())
                ? profileRepository.findAllByOrderByIdAsc(pageable)
                : profileRepository.findByIdGreaterThanOrderByIdAsc(lastId, pageable);

        return profiles.stream()
                .map(this::toUserSyncResponse)
                .toList();
    }

    private ProfileResponse buildFullProfileResponse(Profile profile) {
        ProfileResponse response = profileMapper.toResponse(profile);

        // Map ONLY APPROVED certificates back to the profile wrapper
        List<Certificate> approvedCertificates = approvalRequestRepository.findByProfileId(profile.getId()).stream()
                .filter(req -> req.getStatus() == CertificateStatus.APPROVED)
                .flatMap(req -> req.getCertificates().stream())
                .toList();
        response.setCertificates(certificateMapper.toDtoList(approvedCertificates));

        return enrichWithUserInfo(response, profile.getUserId());
    }

    private ProfileResponse enrichWithUserInfo(ProfileResponse response, String userId) {
        try {
            ApiResponse<UserResponse> apiResponse = authClient.getUserById(userId);
            if (apiResponse != null && apiResponse.data() != null) {
                UserResponse user = apiResponse.data();
                response.setEmail(user.getEmail());
                response.setPhoneNumber(user.getPhoneNumber());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch user info for userId: {}. Error: {}", userId, e.getMessage());
        }
        return response;
    }

    private void enrichProfileDetailsResponse(ProfileDetailsResponse response, Profile profile) {
        try {
            ApiResponse<UserResponse> apiResponse = authClient.getUserById(profile.getUserId());
            if (apiResponse != null && apiResponse.data() != null) {
                UserResponse user = apiResponse.data();
                response.setEmail(user.getEmail());
                response.setPhoneNumber(user.getPhoneNumber());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch user info for userId: {}. Error: {}", profile.getUserId(), e.getMessage());
        }

        // Map ONLY APPROVED certificates back to the profile wrapper
        List<Certificate> approvedCertificates = approvalRequestRepository.findByProfileId(profile.getId()).stream()
                .filter(req -> req.getStatus() == CertificateStatus.APPROVED)
                .flatMap(req -> req.getCertificates().stream())
                .toList();
        response.setCertificates(certificateMapper.toDtoList(approvedCertificates));
    }

    private UserSyncResponse toUserSyncResponse(Profile profile) {
        return UserSyncResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .profilePicture(profile.getProfilePicture())
                .avatar(profile.getAvatar())
                .role(profile.getRole())
                .specialty(profile.getSpecialty())
                .isVerified(profile.getIsVerified())
                .bio(profile.getBio())
                .active(profile.getActive())
                .createdAt(profile.getCreatedAt())
                .lastModifiedAt(profile.getLastModifiedAt())
                .build();
    }

    private void publishProfileUpsertEvent(Profile profile) {
        if (profile == null || profile.getId() == null) {
            return;
        }

        ProfileUpsertEvent event = ProfileUpsertEvent.builder()
                .profileId(profile.getId())
                .build();

        outboxEventPublisher.ifPresentOrElse(
                publisher -> publisher.saveAndPublish(profile.getId(), "Profile", EventType.PROFILE_CREATED, event),
                () -> log.warn("OutboxEventPublisher is unavailable. Skip profile upsert event for profileId={}", profile.getId())
        );
    }

    @Override
    public ProfileResponse verifyProfile(String profileId) {
        log.info("Verifying profile ID: {}", profileId);
        Profile profile = getProfileEntityById(profileId);

        profile.setIsVerified(true);
        Profile savedProfile = profileRepository.save(profile);
        log.info("Profile marked as verified successfully.");

        return buildFullProfileResponse(savedProfile);
    }
}
