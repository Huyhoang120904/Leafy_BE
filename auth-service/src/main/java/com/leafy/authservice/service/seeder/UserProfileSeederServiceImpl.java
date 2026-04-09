package com.leafy.authservice.service.seeder;

import com.leafy.authservice.client.ProfileServiceClient;
import com.leafy.authservice.client.dto.ProfileCreateRequest;
import com.leafy.authservice.dto.response.UserProfileSeederResponse;
import com.leafy.authservice.model.User;
import com.leafy.authservice.repository.UserRepository;
import com.leafy.common.enums.Role;
import com.leafy.common.exception.AppException;
import com.leafy.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Seeds auth users and related profiles using the same persistence flow as
 * registration, excluding OTP verification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserProfileSeederServiceImpl implements UserProfileSeederService {

    static final int MIN_QUANTITY = 1;
    static final int MAX_QUANTITY = 1000;
    static final String DEFAULT_PASSWORD = "Seed@12345";

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    ProfileServiceClient profileServiceClient;

    @Override
    public UserProfileSeederResponse seedUsersAndProfiles(int quantity) {
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }

        int createdUserCount = 0;
        int createdProfileCount = 0;
        int failedUserCount = 0;
        int failedProfileCount = 0;

        long batchSeed = System.currentTimeMillis();

        for (int i = 0; i < quantity; i++) {
            User savedUser;

            try {
                savedUser = userRepository.save(buildSeedUser(batchSeed, i));
                createdUserCount++;
            } catch (Exception e) {
                failedUserCount++;
                log.warn("Failed to create seed user at index {}: {}", i, e.getMessage());
                continue;
            }

            try {
                ProfileCreateRequest profileRequest = ProfileCreateRequest.builder()
                        .userId(savedUser.getId())
                        .fullName(savedUser.getEmail().split("@")[0])
                        .role("FARMER")
                        .build();

                var profileResponse = profileServiceClient.createProfile(profileRequest);
                if (profileResponse != null && profileResponse.data() != null) {
                    createdProfileCount++;
                } else {
                    failedProfileCount++;
                }
            } catch (Exception e) {
                failedProfileCount++;
                log.warn("Failed to create profile for seed user {}: {}", savedUser.getId(), e.getMessage());
            }
        }

        log.info("Seed users/profiles completed - requested={}, usersCreated={}, profilesCreated={}, usersFailed={}, profilesFailed={}",
                quantity, createdUserCount, createdProfileCount, failedUserCount, failedProfileCount);

        return UserProfileSeederResponse.builder()
                .requestedCount(quantity)
                .createdUserCount(createdUserCount)
                .createdProfileCount(createdProfileCount)
                .failedUserCount(failedUserCount)
                .failedProfileCount(failedProfileCount)
                .build();
    }

    private User buildSeedUser(long batchSeed, int index) {
        String email = "seed.user." + batchSeed + "." + index + "@leafy.local";
        String phoneNumber = String.format("0%09d", Math.floorMod(batchSeed + index, 1_000_000_000L));

        return User.builder()
                .email(email)
                .phoneNumber(phoneNumber)
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .role(Role.USER)
                .build();
    }
}