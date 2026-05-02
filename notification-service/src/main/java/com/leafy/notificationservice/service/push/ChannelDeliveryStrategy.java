package com.leafy.notificationservice.service.push;

import com.leafy.notificationservice.enums.NotificationChannel;
import com.leafy.notificationservice.event.ReadyToDeliverEvent;

/**
 * Strategy interface for single-channel notification delivery.
 *
 * <p>Each implementation is responsible for <em>one</em> delivery channel
 * (e.g. FCM, WebSocket/in-app). The orchestrator
 * ({@link com.leafy.notificationservice.service.delivery.NotificationDeliveryServiceImpl})
 * collects all registered strategy beans and dispatches a
 * {@link ReadyToDeliverEvent} to every strategy whose
 * {@link #supports(NotificationChannel)} method returns {@code true} for
 * at least one of the channels declared on the event.
 *
 * <h3>Adding a new channel</h3>
 * <ol>
 *   <li>Add a constant to {@link NotificationChannel}.</li>
 *   <li>Create a class that implements this interface and annotate it as a Spring bean.</li>
 *   <li>Declare the new channel in the {@code channels} set on the {@link ReadyToDeliverEvent}.</li>
 * </ol>
 */
public interface ChannelDeliveryStrategy {

    /** Returns {@code true} when this strategy can handle the given {@code channel}. */
    boolean supports(NotificationChannel channel);

    /**
     * Execute delivery to all applicable recipients / sessions for this channel.
     *
     * <p>Must not throw — transient failures should be logged internally.
     * The calling orchestrator guarantees that sibling strategies are always
     * attempted regardless of this strategy's outcome.
     *
     * @param event the fully-resolved delivery event built by the orchestrator
     */
    void deliver(ReadyToDeliverEvent event);
}
