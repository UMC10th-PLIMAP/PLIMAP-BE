package com.example.plimap.domain.notification.service.command;

public interface NotificationCommandService {

    void createFollowNotification(Long recipientId, Long actorId);

    void createPinCreatedNotifications(Long pinId, Long authorId);

    void createPinLikedNotification(Long recipientId, Long actorId, Long pinId);
}
