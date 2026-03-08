package com.leafy.userservice.dto.request.preferences;

/**
 * Request DTO for updating message settings
 */
public record MessageSettingsUpdateRequest(
        Boolean quickResponseEnable,
        Boolean separatePriorityAndOtherEnable,
        Boolean showTypingStatus
) {
}
