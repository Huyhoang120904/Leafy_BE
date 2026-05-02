package com.leafy.notificationservice.controller;

import com.leafy.common.dto.ApiResponse;
import com.leafy.notificationservice.dto.response.NotificationStateResponse;
import com.leafy.notificationservice.dto.response.UserNotificationResponse;
import com.leafy.notificationservice.model.UserNotification;
import com.leafy.notificationservice.model.UserNotificationState;
import com.leafy.notificationservice.repository.UserNotificationRepository;
import com.leafy.notificationservice.repository.UserNotificationStateRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API for the user-facing notification history and state.
 *
 * <p>All endpoints require the user to be authenticated — the recipient ID
 * is extracted from the JWT principal so users can only see their own notifications.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET  /notifications/history}          — paginated history (cursor-based)</li>
 *   <li>{@code GET  /notifications/history/unread}   — unread notifications only</li>
 *   <li>{@code GET  /notifications/state}            — unread count + lastCheckedAt</li>
 *   <li>{@code POST /notifications/checked}          — update lastCheckedAt to now</li>
 *   <li>{@code POST /notifications/{id}/read}        — mark a single notification read</li>
 *   <li>{@code POST /notifications/read-all}         — mark all notifications read</li>
 * </ul>
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {

    UserNotificationRepository notificationRepository;
    UserNotificationStateRepository stateRepository;
    MongoTemplate mongoTemplate;

    // ── History ─────────────────────────────────────────────────────────────

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<UserNotificationResponse>>> getHistory(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursor,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {

        String userId = authentication.getName();
        PageRequest page = PageRequest.of(0, limit);

        List<UserNotification> items = cursor == null
                ? notificationRepository.findByRecipientIdAndActiveTrueOrderByOccurredAtDesc(userId, page)
                : notificationRepository.findByRecipientIdAndActiveTrueAndOccurredAtBeforeOrderByOccurredAtDesc(userId, cursor, page);

        return ResponseEntity.ok(ApiResponse.success(items.stream().map(this::toResponse).toList()));
    }

    @GetMapping("/history/unread")
    public ResponseEntity<ApiResponse<List<UserNotificationResponse>>> getUnreadHistory(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursor,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {

        String userId = authentication.getName();
        PageRequest page = PageRequest.of(0, limit);

        List<UserNotification> items = cursor == null
                ? notificationRepository.findByRecipientIdAndActiveTrueAndIsReadFalseOrderByOccurredAtDesc(userId, page)
                : notificationRepository.findByRecipientIdAndActiveTrueAndIsReadFalseAndOccurredAtBeforeOrderByOccurredAtDesc(userId, cursor, page);

        return ResponseEntity.ok(ApiResponse.success(items.stream().map(this::toResponse).toList()));
    }

    // ── State ────────────────────────────────────────────────────────────────

    @GetMapping("/state")
    public ResponseEntity<ApiResponse<NotificationStateResponse>> getState(Authentication authentication) {
        String userId = authentication.getName();
        UserNotificationState state = stateRepository.findById(userId)
                .orElse(UserNotificationState.builder().userId(userId).unreadCount(0L).build());

        return ResponseEntity.ok(ApiResponse.success(
                new NotificationStateResponse(state.getUnreadCount(), state.getLastCheckedAt())));
    }

    @PostMapping("/checked")
    public ResponseEntity<ApiResponse<Void>> markChecked(Authentication authentication) {
        String userId = authentication.getName();
        mongoTemplate.upsert(
                new Query(Criteria.where("_id").is(userId)),
                new Update().set("lastCheckedAt", LocalDateTime.now()),
                UserNotificationState.class
        );
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable String id, Authentication authentication) {

        String userId = authentication.getName();
        notificationRepository.findByIdAndRecipientId(id, userId).ifPresent(n -> {
            if (!n.isRead()) {
                n.setRead(true);
                n.setReadAt(LocalDateTime.now());
                notificationRepository.save(n);
                decrementUnreadCount(userId);
            }
        });
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        String userId = authentication.getName();

        // Bulk update — mark all unread, active notifications as read
        mongoTemplate.updateMulti(
                new Query(Criteria.where("recipientId").is(userId)
                        .and("active").is(true)
                        .and("isRead").is(false)),
                new Update().set("isRead", true).set("readAt", LocalDateTime.now()),
                UserNotification.class
        );

        // Reset unread count to 0
        mongoTemplate.upsert(
                new Query(Criteria.where("_id").is(userId)),
                new Update().set("unreadCount", 0L),
                UserNotificationState.class
        );

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private UserNotificationResponse toResponse(UserNotification n) {
        return new UserNotificationResponse(
                n.getId(),
                n.getType(),
                n.getReferenceId(),
                n.getActorId(),
                n.getActorName(),
                n.getActorAvatar(),
                n.getTitle(),
                n.getBody(),
                n.isRead(),
                n.getOccurredAt()
        );
    }

    private void decrementUnreadCount(String userId) {
        mongoTemplate.upsert(
                new Query(Criteria.where("_id").is(userId)),
                new Update().inc("unreadCount", -1L),
                UserNotificationState.class
        );
    }
}
