package com.leafy.userservice.dto.request.preferences;

/**
 * Request DTO for updating sync settings
 */
public record SyncSettingsUpdateRequest(
        Boolean syncSuggestion,
        Boolean showSyncProgress
) {
}
