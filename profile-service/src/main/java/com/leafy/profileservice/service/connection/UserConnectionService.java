package com.leafy.profileservice.service.connection;

import com.leafy.profileservice.dto.response.profile.UserConnectionResponse;
import com.leafy.profileservice.dto.response.profile.ConsultationRequestResponse;
import com.leafy.profileservice.dto.response.profile.ProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserConnectionService {

    UserConnectionResponse followUser(String followerId, String followingId);

    void unfollowUser(String followerId, String followingId);

    UserConnectionResponse requestConsultation(String farmerId, String expertId);

    void cancelConsultationRequest(String farmerId, String expertId);

    UserConnectionResponse respondToConsultationRequest(String expertId, String farmerId, boolean accept);

    List<String> getFollowingUsers(String followerId);

    List<String> getUserFollowers(String followingId);

    Page<ConsultationRequestResponse> getPendingConsultations(String expertId, Pageable pageable);

    Page<ConsultationRequestResponse> getAcceptedConsultations(String expertId, Pageable pageable);

    Page<ProfileResponse> getUserFollowerProfiles(String followingId, Pageable pageable);

    boolean isActiveConsultation(String expertProfileId, String farmerProfileId);

    List<String> getConsultingFarmerIds(String expertProfileId);
}
