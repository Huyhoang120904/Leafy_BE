package com.leafy.authservice.service.seeder;

import com.leafy.authservice.client.ProfileClient;
import com.leafy.authservice.client.dto.CreateProfileRequest;
import com.leafy.authservice.model.User;
import com.leafy.authservice.repository.UserRepository;
import com.leafy.common.dto.client.userservice.user.request.UserCreateRequest;
import com.leafy.common.enums.Role;
import com.leafy.common.model.kafka.EventType;
import com.leafy.common.publisher.OutboxEventPublisher;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AccountSeederServiceImpl implements AccountSeederService {

    static final String DEFAULT_PASSWORD = "Test@123";
    static final String EMAIL_PATTERN = "testuser%d@leafy.com";
    static final String PHONE_PATTERN = "09%08d";

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    ProfileClient profileClient;
    Optional<OutboxEventPublisher> outboxEventPublisher;

    static final String[] BASE_NAMES = {
            "Tran Ngoc Huyen", "Nguyen Thi Mai", "Le Thanh Huong", "Pham Minh Tuan",
            "Vo Hoang Nam", "Bui Quynh Anh", "Do Tien Dung", "Dang Quoc Huy",
            "Pham Thu Thao", "Le Quang Dung", "Ngo Phuong Anh", "Mai Xuan Phong",
            "Nguyen Van An", "Truong Minh Khoa", "Cao Quang Hai", "Duong Van Tung"
    };

    @Override
    public Map<String, Object> seedAccounts(int count) {
        log.info("Starting account seeding for {} records", count);

        int created = 0;
        int skipped = 0;
        int profileCreated = 0;
        int profileFailed = 0;
        int eventsPublished = 0;

        Random random = new Random();
        int startIndex = findNextAvailableIndex();

        for (int i = 0; i < count; i++) {
            int currentIndex = startIndex + i;
            String email = String.format(EMAIL_PATTERN, currentIndex);

            if (userRepository.existsByEmail(email)) {
                skipped++;
                continue;
            }

            try {
                String fullName = generateFuzzyName(random);
                String phoneNumber = String.format(PHONE_PATTERN, currentIndex % 100000000);

                User user = User.builder()
                        .email(email)
                        .phoneNumber(phoneNumber)
                        .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                        .role(Role.USER)
                        .build();

                User savedUser = userRepository.save(user);

                if (createProfile(savedUser, fullName)) {
                    profileCreated++;
                } else {
                    profileFailed++;
                }

                if (publishUserRegisteredEvent(savedUser, fullName)) {
                    eventsPublished++;
                }

                created++;
            } catch (Exception ex) {
                log.error("Failed to seed account with index {}", currentIndex, ex);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("total", count);
        result.put("profileCreated", profileCreated);
        result.put("profileFailed", profileFailed);
        result.put("eventsPublished", eventsPublished);
        result.put("seededPassword", DEFAULT_PASSWORD);
        result.put("nextStartIndex", startIndex + count);
        result.put("message", String.format("Seeding completed. Created=%d, Skipped=%d", created, skipped));

        log.info("Account seeding completed: {}", result);
        return result;
    }

    private boolean createProfile(User user, String fullName) {
        try {
            profileClient.createProfile(CreateProfileRequest.builder()
                    .userId(user.getId())
                    .fullName(fullName)
                    .email(user.getEmail())
                    .phoneNumber(user.getPhoneNumber())
                    .build());
            return true;
        } catch (Exception ex) {
            log.warn("Profile creation failed for userId={}", user.getId(), ex);
            return false;
        }
    }

    private boolean publishUserRegisteredEvent(User user, String fullName) {
        if (outboxEventPublisher.isEmpty()) {
            return false;
        }

        try {
            UserCreateRequest payload = UserCreateRequest.builder()
                    .accountId(user.getId())
                    .fullName(fullName)
                    .build();

            outboxEventPublisher.get().saveAndPublish(
                    user.getId(),
                    "User",
                    EventType.USER_REGISTERED,
                    payload
            );
            return true;
        } catch (Exception ex) {
            log.warn("Failed to publish USER_REGISTERED event for userId={}", user.getId(), ex);
            return false;
        }
    }

    private int findNextAvailableIndex() {
        int index = 1;
        while (userRepository.existsByEmail(String.format(EMAIL_PATTERN, index))) {
            index++;
            if (index > 200000) {
                break;
            }
        }
        return index;
    }

    private String generateFuzzyName(Random random) {
        String baseName = BASE_NAMES[random.nextInt(BASE_NAMES.length)];
        int style = random.nextInt(5);

        return switch (style) {
            case 0 -> baseName;
            case 1 -> baseName.toLowerCase();
            case 2 -> baseName.toUpperCase();
            case 3 -> removeAccents(baseName);
            default -> baseName.replace(" ", random.nextBoolean() ? "  " : " ").trim();
        };
    }

    private String removeAccents(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
    }
}
