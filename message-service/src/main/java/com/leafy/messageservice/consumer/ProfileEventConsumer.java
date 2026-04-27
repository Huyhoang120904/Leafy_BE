package com.leafy.messageservice.consumer;

import com.leafy.common.event.profile.ProfileEvent;
import com.leafy.messageservice.model.ChatUser;
import com.leafy.messageservice.repository.ChatUserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Kafka consumer that keeps the local {@code chat_users} buffer collection
 * in sync with profile data from the profile-service.
 *
 * <p>On {@code profile.created} and {@code profile.updated} events, the
 * corresponding {@link ChatUser} document is upserted (created if absent,
 * or updated in place) so that message enrichment — displaying the sender's
 * name and avatar — never requires a synchronous call to the profile-service.
 *
 * <p>On {@code profile.deleted}, the {@link ChatUser} document is removed.
 *
 * <p>The consumer group ID is the application name ({@code message-service}),
 * which means every instance of the service processes the same partition
 * assignment in a coordinated way and no events are duplicated across instances.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileEventConsumer {

    ChatUserRepository chatUserRepository;

    // ──────────────────────────── Created ────────────────────────────

    @KafkaListener(
            topics = "#{kafkaTopicProperties.profileEvents.created}",
            groupId = "${spring.application.name}",
            containerFactory = "profileEventKafkaListenerContainerFactory"
    )
    public void handleProfileCreated(ProfileEvent event) {
        log.info("[Kafka] ProfileCreated received: profileId={}, userId={}", event.getProfileId(), event.getUserId());
        upsertChatUser(event);
    }

    // ──────────────────────────── Updated ────────────────────────────

    @KafkaListener(
            topics = "#{kafkaTopicProperties.profileEvents.updated}",
            groupId = "${spring.application.name}",
            containerFactory = "profileEventKafkaListenerContainerFactory"
    )
    public void handleProfileUpdated(ProfileEvent event) {
        log.info("[Kafka] ProfileUpdated received: profileId={}, userId={}", event.getProfileId(), event.getUserId());
        upsertChatUser(event);
    }

    // ──────────────────────────── Deleted ────────────────────────────

    @KafkaListener(
            topics = "#{kafkaTopicProperties.profileEvents.deleted}",
            groupId = "${spring.application.name}",
            containerFactory = "profileEventKafkaListenerContainerFactory"
    )
    public void handleProfileDeleted(ProfileEvent event) {
        log.info("[Kafka] ProfileDeleted received: profileId={}, userId={}", event.getProfileId(), event.getUserId());
        if (event.getUserId() == null) {
            log.warn("[Kafka] ProfileDeleted event missing userId — cannot remove ChatUser. profileId={}", event.getProfileId());
            return;
        }
        chatUserRepository.deleteById(event.getUserId());
        log.info("[Kafka] ChatUser removed for userId={}", event.getUserId());
    }

    // ──────────────────────────── Helpers ────────────────────────────

    /**
     * Upserts a {@link ChatUser} document using the auth {@code userId} as the
     * MongoDB {@code _id}.  Only the display-relevant fields (name, avatar) are
     * written; presence fields are left unchanged on update so
     * that they are not inadvertently reset.
     */
    private void upsertChatUser(ProfileEvent event) {
        if (event.getUserId() == null) {
            log.warn("[Kafka] ProfileEvent missing userId — cannot upsert ChatUser. profileId={}", event.getProfileId());
            return;
        }

        ChatUser chatUser = chatUserRepository.findById(event.getUserId())
                .orElse(ChatUser.builder()
                        .id(event.getUserId())
                        .build());

        chatUser.setFullName(event.getFullName());
        chatUser.setAvatar(event.getAvatar());
        chatUser.setLastUpdatedAt(LocalDateTime.now());

        chatUserRepository.save(chatUser);
        log.info("[Kafka] ChatUser upserted: userId={}, fullName={}", event.getUserId(), event.getFullName());
    }
}
