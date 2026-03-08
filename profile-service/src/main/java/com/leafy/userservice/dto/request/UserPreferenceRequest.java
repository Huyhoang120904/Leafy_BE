package com.leafy.userservice.dto.request;

import com.leafy.userservice.model.UserPreference;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * User preference request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserPreferenceRequest {

    UserPreference.GeneralSettings generalSettings;
    UserPreference.SecuritySettings securitySettings;
    UserPreference.PrivacySettings privacySettings;
    UserPreference.SyncSettings syncSettings;
    UserPreference.AppearanceSettings appearanceSettings;
    UserPreference.MessageSettings messageSettings;
    UserPreference.NotificationSettings notificationSettings;
    UserPreference.UtilitiesSettings utilitiesSettings;
}
