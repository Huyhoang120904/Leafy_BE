package com.leafy.communityfeedservice.service.post;

import com.leafy.common.exception.AppException;
import com.leafy.common.exception.ErrorCode;
import com.leafy.common.utils.ServiceSecurityUtils;
import com.leafy.communityfeedservice.client.PlantManagementServiceClient;
import com.leafy.communityfeedservice.client.dto.ExternalApiResponse;
import com.leafy.communityfeedservice.client.dto.PlanSummaryResponse;
import com.leafy.communityfeedservice.dto.request.PostCreateRequest;
import com.leafy.communityfeedservice.dto.request.PostUpdateRequest;
import com.leafy.communityfeedservice.dto.response.PlanInfo;
import com.leafy.communityfeedservice.dto.response.PostResponse;
import com.leafy.communityfeedservice.mapper.PostMapper;
import com.leafy.communityfeedservice.model.Post;
import com.leafy.communityfeedservice.model.ProfileSummary;
import com.leafy.communityfeedservice.model.Vote;
import com.leafy.communityfeedservice.model.embedded.PostStats;
import com.leafy.communityfeedservice.model.enums.PostType;
import com.leafy.communityfeedservice.model.enums.VoteTargetType;
import com.leafy.communityfeedservice.repository.PostRepository;
import com.leafy.communityfeedservice.repository.ProfileSummaryRepository;
import com.leafy.communityfeedservice.repository.VoteRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostServiceImpl implements PostService {

    private static final List<PostType> FEED_AND_SHARED_TYPES = List.of(PostType.FEED, PostType.SHARE, PostType.PLAN_SHARE);

    PostRepository postRepository;
    PostMapper postMapper;
    ProfileSummaryRepository profileSummaryRepository;
    VoteRepository voteRepository;
    Optional<PlantManagementServiceClient> plantManagementServiceClient;


    @Override
    @Transactional
    public PostResponse createPost(PostCreateRequest request) {
        validatePostTypeConstraints(request);

        String currentProfileId = ServiceSecurityUtils.getCurrentProfileId();

        Post post = postMapper.toEntity(request);
        post.setAuthorId(currentProfileId);
        post.setStats(new PostStats());

        post = postRepository.save(post);
        return enrichPostResponse(postMapper.toResponse(post));
    }

    private void validatePostTypeConstraints(PostCreateRequest request) {
        if (request.postType() == PostType.SHARE) {
            if (request.sharedPostId() == null || request.sharedPostId().isBlank()) {
                throw new AppException(ErrorCode.INVALID_POST_TYPE_CONSTRAINT);
            }
            if (request.originalAuthorId() == null || request.originalAuthorId().isBlank()) {
                throw new AppException(ErrorCode.INVALID_POST_TYPE_CONSTRAINT);
            }
        }
        if (request.postType() == PostType.PLAN_SHARE) {
            if (request.planId() == null || request.planId().isBlank()) {
                throw new AppException(ErrorCode.INVALID_POST_TYPE_CONSTRAINT);
            }
        }
    }

    @Override
    public PostResponse getPostById(String id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SYS_UNCATEGORIZED)); 
        return enrichPostResponse(postMapper.toResponse(post));
    }

    @Override
    public Page<PostResponse> getAllFeedAndSharedPosts(Pageable pageable) {
        Page<Post> postPage = postRepository.findByPostTypeIn(FEED_AND_SHARED_TYPES, pageable);
        List<PostResponse> responses = postPage.getContent().stream()
                .map(postMapper::toResponse)
                .collect(Collectors.toList());
        enrichPostResponses(responses);
        return new PageImpl<>(responses, pageable, postPage.getTotalElements());
    }

    @Override
    public Page<PostResponse> getPostsByUserId(String userId, Pageable pageable) {
        Page<Post> postPage = postRepository.findByAuthorId(userId, pageable);
        List<PostResponse> responses = postPage.getContent().stream()
                .map(postMapper::toResponse)
                .collect(Collectors.toList());
        enrichPostResponses(responses);
        return new PageImpl<>(responses, pageable, postPage.getTotalElements());
    }

    @Override
    @Transactional
    public PostResponse updatePost(String id, PostUpdateRequest request) {
        String currentProfileId = ServiceSecurityUtils.getCurrentProfileId();
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SYS_UNCATEGORIZED));
        
        if (!post.getAuthorId().equals(currentProfileId)) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        postMapper.updateEntityFromRequest(request, post);
        post.setEdited(true);
        post = postRepository.save(post);
        return enrichPostResponse(postMapper.toResponse(post));
    }

    @Override
    @Transactional
    public void deletePost(String id) {
        String currentProfileId = ServiceSecurityUtils.getCurrentProfileId();
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SYS_UNCATEGORIZED));
        
        if (!post.getAuthorId().equals(currentProfileId)) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        
        postRepository.delete(post);
    }

    private PostResponse enrichPostResponse(PostResponse response) {
        profileSummaryRepository.findById(response.getAuthorId())
                .ifPresent(response::setAuthorInfo);

        if (response.getSharedPostId() != null) {
            postRepository.findById(response.getSharedPostId()).ifPresent(sharedPost -> {
                PostResponse sharedResponse = postMapper.toResponse(sharedPost);
                profileSummaryRepository.findById(sharedPost.getAuthorId())
                        .ifPresent(sharedResponse::setAuthorInfo);
                response.setSharedPostInfo(sharedResponse);
            });
        }

        String currentProfileId = ServiceSecurityUtils.getCurrentProfileId();
        if (currentProfileId != null) {
            voteRepository.findByAuthorIdAndTargetIdAndTargetType(
                    currentProfileId, response.getId(), VoteTargetType.POST)
                    .filter(Vote::isActive)
                    .ifPresent(vote -> response.setCurrentUserVoteType(vote.getType()));
        }

        // Embed plan snapshot for PLAN_SHARE posts
        if (PostType.PLAN_SHARE.equals(response.getPostType()) && response.getPlanId() != null) {
            PlanInfo planInfo = fetchPlanInfo(response.getPlanId());
            response.setPlanInfo(planInfo);
        }

        return response;
    }

    /** Fetch a single plan from plant-management-service and map to PlanInfo. Returns null on failure. */
    private PlanInfo fetchPlanInfo(String planId) {
        if (plantManagementServiceClient.isEmpty() || planId == null) return null;
        try {
            ExternalApiResponse<PlanSummaryResponse> resp =
                    plantManagementServiceClient.get().getPlanById(planId);
            if (resp == null || resp.getData() == null) return null;
            return toPlanInfo(resp.getData());
        } catch (Exception ex) {
            log.warn("Could not fetch planInfo for planId={}: {}", planId, ex.getMessage());
            return null;
        }
    }

    private PlanInfo toPlanInfo(PlanSummaryResponse src) {
        if (src == null) return null;
        return PlanInfo.builder()
                .id(src.getId())
                .planName(src.getPlanName())
                .diseaseName(src.getDiseaseName())
                .severityLevel(src.getSeverityLevel())
                .urgency(src.getUrgency())
                .status(src.getStatus())
                .estimatedCost(src.getEstimatedCost())
                .confidenceScore(src.getConfidenceScore())
                .requiredInputs(src.getRequiredInputs())
                .safetyWarnings(src.getSafetyWarnings())
                .successIndicators(src.getSuccessIndicators())
                .applyCount(src.getApplyCount())
                .eventCount(src.getPlantEventIds() != null ? src.getPlantEventIds().size() : 0)
                .isPublic(src.isPublic())
                .createdAt(src.getCreatedAt())
                .build();
    }

    private void enrichPostResponses(List<PostResponse> responses) {
        // Collect all author IDs and shared post IDs
        Set<String> profileIds = new HashSet<>();
        Set<String> sharedPostIds = new HashSet<>();

        for (PostResponse r : responses) {
            if (r.getAuthorId() != null) profileIds.add(r.getAuthorId());
            if (r.getOriginalAuthorId() != null) profileIds.add(r.getOriginalAuthorId());
            if (r.getSharedPostId() != null) sharedPostIds.add(r.getSharedPostId());
        }

        // Batch fetch profiles
        Map<String, ProfileSummary> profileMap = profileSummaryRepository
                .findAllByIdIn(new ArrayList<>(profileIds)).stream()
                .collect(Collectors.toMap(ProfileSummary::getId, Function.identity()));

        // Batch fetch shared posts
        Map<String, Post> sharedPostMap = sharedPostIds.isEmpty()
                ? Collections.emptyMap()
                : postRepository.findAllById(sharedPostIds).stream()
                        .collect(Collectors.toMap(Post::getId, Function.identity()));

        // Collect shared post author IDs for a second profile batch
        Set<String> sharedAuthorIds = sharedPostMap.values().stream()
                .map(Post::getAuthorId)
                .filter(id -> id != null && !profileMap.containsKey(id))
                .collect(Collectors.toSet());

        if (!sharedAuthorIds.isEmpty()) {
            profileSummaryRepository.findAllByIdIn(new ArrayList<>(sharedAuthorIds))
                    .forEach(p -> profileMap.put(p.getId(), p));
        }

        // Batch fetch current user's votes for all posts
        String currentProfileId = ServiceSecurityUtils.getCurrentProfileId();
        Map<String, Vote> voteMap = Collections.emptyMap();
        if (currentProfileId != null) {
            List<String> postIds = responses.stream()
                    .map(PostResponse::getId)
                    .collect(Collectors.toList());
            voteMap = voteRepository.findByAuthorIdAndTargetIdInAndTargetTypeAndActiveTrue(
                    currentProfileId, postIds, VoteTargetType.POST).stream()
                    .collect(Collectors.toMap(Vote::getTargetId, Function.identity()));
        }

        // Enrich each response
        for (PostResponse r : responses) {
            r.setAuthorInfo(profileMap.get(r.getAuthorId()));

            if (r.getSharedPostId() != null) {
                Post sharedPost = sharedPostMap.get(r.getSharedPostId());
                if (sharedPost != null) {
                    PostResponse sharedResponse = postMapper.toResponse(sharedPost);
                    sharedResponse.setAuthorInfo(profileMap.get(sharedPost.getAuthorId()));
                    r.setSharedPostInfo(sharedResponse);
                }
            }

            Vote vote = voteMap.get(r.getId());
            if (vote != null) {
                r.setCurrentUserVoteType(vote.getType());
            }

            // Embed plan snapshot for PLAN_SHARE posts
            if (PostType.PLAN_SHARE.equals(r.getPostType()) && r.getPlanId() != null) {
                r.setPlanInfo(fetchPlanInfo(r.getPlanId()));
            }
        }
    }
}

