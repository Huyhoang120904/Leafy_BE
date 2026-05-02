package com.leafy.notificationservice.service.push;

import com.leafy.notificationservice.enums.NotificationChannel;
import com.leafy.notificationservice.event.ReadyToDeliverEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * In-app (WebSocket) {@link ChannelDeliveryStrategy} — handles {@link NotificationChannel#IN_APP}.
 *
 * <p>Currently a no-op stub. A real implementation would publish to
 * {@code socket-service} via Feign or a dedicated Kafka topic.
 *
 * <p>Registered unconditionally as a {@code @Component} — no Firebase dependency required.
 */
@Slf4j
@Component
public class InAppDeliveryStrategy implements ChannelDeliveryStrategy {

    @Override
    public boolean supports(NotificationChannel channel) {
        return NotificationChannel.IN_APP == channel;
    }

    @Override
    public void deliver(ReadyToDeliverEvent event) {
        log.debug("[IN_APP] WebSocket channel not yet configured — skipping: recipient={}, type={}",
                event.getRecipientId(), event.getType());
    }
}
