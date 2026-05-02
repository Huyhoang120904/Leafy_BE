package com.leafy.notificationservice.model;

import com.leafy.common.enums.NotificationType;
import com.leafy.notificationservice.enums.NotificationChannel;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB-backed notification template.
 *
 * <p>Templates use Mustache-style variables: {@code {{actorName}}}, {@code {{commentPreview}}}.
 * Conditional blocks: {@code {{#condition}}content{{/condition}}}.
 *
 * <p>Unique index on {@code (type, channel, locale)} ensures at most one active template
 * per combination.
 *
 * <p>Collection: {@code notification_templates}
 */
@Document("notification_templates")
@CompoundIndex(
        name = "type_channel_locale_unique",
        def = "{'type': 1, 'channel': 1, 'locale': 1}",
        unique = true
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {

    @Id
    private String id;

    private NotificationType type;

    private NotificationChannel channel;

    /** Locale tag, e.g. {@code "vi"} or {@code "en"}. */
    private String locale;

    /**
     * Title template, e.g. {@code "Leafy"} or {@code "{{actorName}} đã bình luận"}.
     * May reference payload variables.
     */
    private String titleTemplate;

    /**
     * Body template, e.g. {@code "{{actorName}} đã bình luận bài viết của bạn"}.
     */
    private String bodyTemplate;

    @Builder.Default
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
