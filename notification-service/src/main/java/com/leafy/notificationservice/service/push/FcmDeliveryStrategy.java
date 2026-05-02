package com.leafy.notificationservice.service.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.leafy.common.exception.AppException;
import com.leafy.common.exception.ErrorCode;
import com.leafy.notificationservice.enums.NotificationChannel;
import com.leafy.notificationservice.event.ReadyToDeliverEvent;
import com.leafy.notificationservice.model.TokenDevice;
import com.leafy.notificationservice.repository.PushTokenRepository;
import com.leafy.notificationservice.service.token.PushTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;
import java.util.Map;

/**
 * FCM-backed {@link ChannelDeliveryStrategy} — handles {@link NotificationChannel#FCM}.
 *
 * <h3>Responsibilities</h3>
 * <ol>
 *   <li>Queries active FCM tokens for the recipient live (catches newly registered tokens).</li>
 *   <li>Sends each token via {@link FirebaseMessaging} with exponential-backoff retry.</li>
 *   <li>Deactivates stale tokens on permanent FCM errors ({@code UNREGISTERED} / {@code INVALID_ARGUMENT}).</li>
 * </ol>
 *
 * <h3>Retry policy</h3>
 * <ul>
 *   <li>3 max attempts, backoff: 500 ms → 1 000 ms → 2 000 ms (multiplier 2, cap 4 000 ms)</li>
 *   <li>Retries on any {@link RuntimeException} (transient FCM errors)</li>
 *   <li>Permanent errors short-circuit immediately — NOT retried</li>
 * </ul>
 *
 * <p>Registered as a Spring bean by
 * {@link com.leafy.notificationservice.config.PushDeliveryConfig} only when a
 * {@link FirebaseMessaging} bean is available.
 */
@Slf4j
@RequiredArgsConstructor
public class FcmDeliveryStrategy implements ChannelDeliveryStrategy {

    private static final RetryTemplate FCM_RETRY = RetryTemplate.builder()
            .maxAttempts(3)
            .exponentialBackoff(500, 2, 4000)
            .retryOn(RuntimeException.class)
            .build();

    private final FirebaseMessaging firebaseMessaging;
    private final PushTokenRepository pushTokenRepository;
    private final PushTokenService pushTokenService;

    @Override
    public boolean supports(NotificationChannel channel) {
        return NotificationChannel.FCM == channel;
    }

    @Override
    public void deliver(ReadyToDeliverEvent event) {
        List<TokenDevice> tokens = pushTokenRepository.findByUserIdAndActiveTrue(event.getRecipientId());
        if (tokens.isEmpty()) {
            log.debug("[FCM] No active push tokens for recipient={}", event.getRecipientId());
            return;
        }

        for (TokenDevice token : tokens) {
            try {
                String messageId = sendToToken(token.getFcmToken(), event.getTitle(), event.getBody(), event.getFcmData());
                log.debug("[FCM] Push sent: recipient={}, tokenId={}, messageId={}",
                        event.getRecipientId(), token.getId(), messageId);
            } catch (AppException ex) {
                String errorCode = ex.getDetail();
                if (isStaleTokenError(errorCode)) {
                    pushTokenService.deactivateToken(token.getFcmToken());
                    log.warn("[FCM] Deactivated stale token: tokenId={}, code={}", token.getId(), errorCode);
                } else {
                    log.warn("[FCM] Push failed (non-critical): recipient={}, tokenId={}, code={}",
                            event.getRecipientId(), token.getId(), errorCode);
                }
            }
        }
    }

    private String sendToToken(String token, String title, String body, Map<String, String> data) {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data != null ? data : Map.of())
                .build();

        try {
            return FCM_RETRY.execute(ctx -> {
                try {
                    String messageId = firebaseMessaging.send(message);
                    if (ctx.getRetryCount() > 0) {
                        log.info("[FCM] Delivery succeeded after {} retries: token={}", ctx.getRetryCount(), token);
                    }
                    return messageId;
                } catch (FirebaseMessagingException e) {
                    MessagingErrorCode code = e.getMessagingErrorCode();
                    if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                        log.warn("[FCM] Permanent failure (no retry): token={}, code={}", token, code.name());
                        throw new PermanentFcmFailureException(code.name(), e.getMessage(), e);
                    }
                    log.warn("[FCM] Transient error [attempt {}]: message={}", ctx.getRetryCount() + 1, e.getMessage());
                    throw new RuntimeException("FCM transient error: " + e.getMessage(), e);
                }
            });
        } catch (PermanentFcmFailureException e) {
            throw new AppException(ErrorCode.PUSH_DELIVERY_FAILED, e.getErrorCode());
        } catch (Exception e) {
            log.error("[FCM] Delivery failed after all retries: token={}, error={}", token, e.getMessage());
            throw new AppException(ErrorCode.PUSH_DELIVERY_FAILED, e.getMessage());
        }
    }

    private boolean isStaleTokenError(String errorCode) {
        return "UNREGISTERED".equals(errorCode) || "INVALID_ARGUMENT".equals(errorCode);
    }

    private static class PermanentFcmFailureException extends RuntimeException {
        private final String errorCode;
        PermanentFcmFailureException(String errorCode, String message, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
        }
        String getErrorCode() { return errorCode; }
    }
}
