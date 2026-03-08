package com.leafy.userservice.repository;

import com.leafy.userservice.model.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Profile entity
 */
@Repository
public interface ProfileRepository extends MongoRepository<Profile, String> {

    /**
     * Find profile by user ID
     *
     * @param userId the user ID to search for
     * @return optional profile
     */
    Optional<Profile> findByUserId(String userId);

    /**
     * Find profile by email
     *
     * @param email the email to search for
     * @return optional profile
     */
    Optional<Profile> findByEmail(String email);

    /**
     * Find profile by phone number
     *
     * @param phoneNumber the phone number to search for
     * @return optional profile
     */
    Optional<Profile> findByPhoneNumber(String phoneNumber);

    /**
     * Check if profile exists for user ID
     *
     * @param userId the user ID to check
     * @return true if exists, false otherwise
     */
    boolean existsByUserId(String userId);

    /**
     * Check if email exists
     *
     * @param email the email to check
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Check if phone number exists
     *
     * @param phoneNumber the phone number to check
     * @return true if exists, false otherwise
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Find all active profiles with pagination
     *
     * @param pageable pagination information
     * @return page of active profiles
     */
    Page<Profile> findByActiveTrue(Pageable pageable);

    /**
     * Search profiles by full name, email, or phone number
     *
     * @param searchTerm search term
     * @param pageable   pagination information
     * @return page of matching profiles
     */
    @Query("{ '$or': [ " +
            "{ 'fullName': { '$regex': ?0, '$options': 'i' } }, " +
            "{ 'email': { '$regex': ?0, '$options': 'i' } }, " +
            "{ 'phoneNumber': { '$regex': ?0, '$options': 'i' } } " +
            "] }")
    Page<Profile> searchProfiles(String searchTerm, Pageable pageable);

    /**
     * Delete profile by user ID
     *
     * @param userId the user ID
     */
    void deleteByUserId(String userId);
}
