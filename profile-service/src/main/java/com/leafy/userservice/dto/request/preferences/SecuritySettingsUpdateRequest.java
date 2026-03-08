package com.leafy.userservice.dto.request.preferences;

/**
 * Request DTO for updating security settings
 */
public record SecuritySettingsUpdateRequest(
        Boolean twoFactorEnabled
) {
}
