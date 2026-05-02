package com.leafy.notificationservice.service.push;

import com.leafy.notificationservice.enums.NotificationChannel;
import com.leafy.notificationservice.event.ReadyToDeliverEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * No-op fallback {@link ChannelDeliveryStrategy} for {@link NotificationChannel#FCM}.
 *
 * <p>Registered by {@link com.leafy.notificationservice.config.PushDeliveryConfig}
 * when Firebase is disabled or credentials are not configured.
 */
@Slf4j
public class NoOpFcmDeliveryStrategy implements ChannelDeliveryStrategy {

    @Override
    public boolean supports(NotificationChannel channel) {
        return NotificationChannel.FCM == channel;
    }

    @Override
    public void deliver(ReadyToDeliverEvent event) {
        log.debug("[FCM] Push delivery disabled — skipping: recipient={}", event.getRecipientId());
    }
}
