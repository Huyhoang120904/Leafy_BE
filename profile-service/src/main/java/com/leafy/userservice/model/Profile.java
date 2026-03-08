package com.leafy.userservice.model;

import com.leafy.common.model.BaseModel;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

/**
 * Profile model
 * Stores user profile information including personal details and preferences
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
@Document("profile")
public class Profile extends BaseModel {
    
    @MongoId
    String id;
    
    /**
     * User ID from auth service (references User entity)
     */
    @Indexed(unique = true)
    String userId;
    
    /**
     * User's full name
     */
    String fullName;
    
    /**
     * URL to user's profile picture
     */
    String profilePicture;
    
    /**
     * URL to user's certificate document
     */
    String certificate;
    
    /**
     * User's biography or description
     */
    String bio;
    
    /**
     * User preferences (embedded document)
     */
    UserPreference userPreference;
    
    /**
     * User's email address
     */
    @Indexed
    String email;
    
    /**
     * User's phone number
     */
    String phoneNumber;
}
