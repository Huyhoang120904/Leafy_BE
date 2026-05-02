package com.leafy.notificationservice.config;

import com.leafy.common.enums.NotificationType;
import com.leafy.notificationservice.enums.NotificationChannel;
import com.leafy.notificationservice.model.NotificationTemplate;
import com.leafy.notificationservice.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds default Vietnamese FCM notification templates on first startup.
 * Skips any template where the (type, channel, locale) combination already exists.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationTemplateSeeder implements CommandLineRunner {

    private final NotificationTemplateRepository templateRepository;

    @Override
    public void run(String... args) {
        LocalDateTime now = LocalDateTime.now();
        String locale = "vi";

        List<SeedEntry> seeds = List.of(
                new SeedEntry(NotificationType.POST_COMMENT,   "Leafy", "{{actorName}} đã bình luận bài viết của bạn"),
                new SeedEntry(NotificationType.POST_UPVOTE,    "Leafy", "{{actorName}} đã thích bài viết của bạn"),
                new SeedEntry(NotificationType.COMMENT_REPLY,  "Leafy", "{{actorName}} đã trả lời bình luận của bạn"),
                new SeedEntry(NotificationType.COMMENT_UPVOTE, "Leafy", "{{actorName}} đã thích bình luận của bạn"),
                new SeedEntry(NotificationType.USER_FOLLOW,    "Leafy", "{{actorName}} đã theo dõi bạn"),
                new SeedEntry(NotificationType.SYSTEM,         "Leafy", "{{body}}")
        );

        for (SeedEntry seed : seeds) {
            boolean exists = templateRepository
                    .findByTypeAndChannelAndLocaleAndActiveTrue(seed.type, NotificationChannel.FCM, locale)
                    .isPresent();

            if (!exists) {
                templateRepository.save(NotificationTemplate.builder()
                        .type(seed.type)
                        .channel(NotificationChannel.FCM)
                        .locale(locale)
                        .titleTemplate(seed.title)
                        .bodyTemplate(seed.body)
                        .active(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
                log.info("[Seeder] Inserted template: type={}, locale={}", seed.type, locale);
            }
        }
    }

    private record SeedEntry(NotificationType type, String title, String body) {}
}
