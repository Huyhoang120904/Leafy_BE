package com.leafy.userservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Response DTO for profile information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileResponse {
    
    String id;
    
    String userId;
    
    String fullName;
    
    String profilePicture;
    
    String certificate;
    
    String bio;
    
    UserPreferenceResponse userPreference;
    
    String email;
    
    String phoneNumber;
    
    boolean active;
    
    LocalDateTime createdAt;
    
    LocalDateTime lastModifiedAt;
}
