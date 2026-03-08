package com.leafy.userservice.dto.response;

import com.leafy.userservice.model.UserPreference;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * User preference response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserPreferenceResponse {

    UserPreference.GeneralSettings generalSettings;
    UserPreference.SecuritySettings securitySettings;
    UserPreference.PrivacySettings privacySettings;
    UserPreference.SyncSettings syncSettings;
    UserPreference.AppearanceSettings appearanceSettings;
    UserPreference.MessageSettings messageSettings;
    UserPreference.NotificationSettings notificationSettings;
    UserPreference.UtilitiesSettings utilitiesSettings;
}
