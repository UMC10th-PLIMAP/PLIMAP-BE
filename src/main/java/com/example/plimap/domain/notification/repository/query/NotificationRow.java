package com.example.plimap.domain.notification.repository.query;

import com.example.plimap.domain.notification.entity.Notification;

public record NotificationRow(
        Notification notification,
        boolean isFollowing,
        boolean isFollowingViewer
) {
}
