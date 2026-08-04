package com.example.plimap.domain.notification.repository.query;

import com.example.plimap.domain.notification.dto.Pagination;
import com.example.plimap.domain.notification.entity.Notification;

public interface NotificationQueryRepository {

    Pagination<Notification> findNotifications(Long memberId, String cursor, Integer pageSize);
}
