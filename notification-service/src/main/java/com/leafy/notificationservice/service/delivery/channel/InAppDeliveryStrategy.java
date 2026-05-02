package com.leafy.notificationservice.service.delivery.channel;

import com.leafy.notificationservice.enums.NotificationChannel;
import com.leafy.notificationservice.event.ReadyToDeliverEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * In-app (WebSocket) {@link ChannelDeliveryStrategy} — handles {@link NotificationChannel#IN_APP}.
 *
 * <p>Currently a no-op stub. A real implementation would publish to
 * {@code socket-service} via Feign or a dedicated Kafka topic to push a
 * real-time badge-update event to the recipient's active WebSocket session.
 *
 * <p>Registered unconditionally as a Spring component — no Firebase dependency required.
 */
@Slf4j
@Component
public class InAppDeliveryStrategy implements ChannelDeliveryStrategy {

    @Override
    public boolean supports(NotificationChannel channel) {
        return NotificationChannel.IN_APP == channel;
    }

    /**
     * Sends a real-time in-app notification to the recipient's active session.
     *
     * <p><b>Current status:</b> no-op stub — socket-service integration will be
     * wired in a later iteration.
     */
    @Override
    public void deliver(ReadyToDeliverEvent event) {
        log.debug("[IN_APP] WebSocket channel not yet configured — skipping real-time push: recipient={}, type={}",
                event.getRecipientId(), event.getType());
    }
}
