package com.leafy.notificationservice.repository;

import com.leafy.notificationservice.document.NotificationLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationLogRepository extends MongoRepository<NotificationLogDocument, String> {
    boolean existsByEventIdAndUserIdAndPushTokenId(String eventId, String userId, String pushTokenId);
}