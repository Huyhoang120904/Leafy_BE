package com.leafy.userservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Detailed response DTO for profile information (includes audit fields)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileDetailsResponse {
    
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
    
    String createdBy;
    
    String lastModifiedBy;
}
