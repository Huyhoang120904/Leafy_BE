package com.leafy.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO for updating an existing profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileUpdateRequest {
    
    String fullName;
    
    String profilePicture;
    
    String certificate;
    
    String bio;
    
    UserPreferenceRequest userPreference;
    
    @Email(message = "Email must be valid")
    String email;
    
    @Pattern(regexp = "^(\\+84|0)[0-9]{9}$", message = "Phone number must be valid Vietnamese phone number")
    String phoneNumber;
}
