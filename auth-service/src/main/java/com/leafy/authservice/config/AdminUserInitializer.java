package com.leafy.authservice.config;

import com.leafy.authservice.client.ProfileClient;
import com.leafy.authservice.client.dto.CreateProfileRequest;
import com.leafy.authservice.model.User;
import com.leafy.authservice.repository.UserRepository;
import com.leafy.common.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes the default admin user on service startup.
 * Idempotent: skips creation if the admin account already exists.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements ApplicationRunner {

    @Value("${app.admin.email:admin@leafy.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@12345}")
    private String adminPassword;

    @Value("${app.admin.phone:0900000000}")
    private String adminPhone;

    @Value("${app.admin.full-name:System Administrator}")
    private String adminFullName;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileClient profileClient;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin user '{}' already exists, skipping initialization", adminEmail);
            return;
        }

        log.info("Admin user not found — creating admin account: {}", adminEmail);

        User adminUser = User.builder()
                .email(adminEmail)
                .phoneNumber(adminPhone)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build();

        User savedUser = userRepository.save(adminUser);
        log.info("Admin user created with ID: {}", savedUser.getId());

        createAdminProfile(savedUser);
    }

    private void createAdminProfile(User adminUser) {
        try {
            profileClient.createProfile(CreateProfileRequest.builder()
                    .userId(adminUser.getId())
                    .fullName(adminFullName)
                    .email(adminUser.getEmail())
                    .phoneNumber(adminUser.getPhoneNumber())
                    .build());
            log.info("Admin profile created successfully for user ID: {}", adminUser.getId());
        } catch (Exception ex) {
            log.warn("Admin profile creation failed (profile-service may not be ready yet): {}", ex.getMessage());
        }
    }
}
