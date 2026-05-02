package com.leafy.notificationservice.dto.request;

import com.leafy.common.enums.NotificationType;
import com.leafy.notificationservice.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateNotificationTemplateRequest {

    @NotNull
    NotificationType type;

    @NotNull
    NotificationChannel channel;

    @NotBlank
    String locale;

    @NotBlank
    String titleTemplate;

    @NotBlank
    String bodyTemplate;
}
