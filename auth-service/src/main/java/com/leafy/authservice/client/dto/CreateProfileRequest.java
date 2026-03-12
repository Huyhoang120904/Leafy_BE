package com.leafy.authservice.client.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO for creating a profile in profile-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateProfileRequest {

    String userId;
}
