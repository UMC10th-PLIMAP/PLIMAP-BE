package com.example.plimap.domain.notification.repository.query;

import com.example.plimap.domain.notification.dto.Pagination;

public interface NotificationQueryRepository {

    Pagination<NotificationRow> findNotifications(Long memberId, String cursor, Integer pageSize);
}
