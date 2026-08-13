package com.example.plimap.domain.notification.service.query;

import com.example.plimap.domain.notification.dto.Pagination;
import com.example.plimap.domain.notification.dto.response.NotificationResponse;

public interface NotificationQueryService {

    Pagination<NotificationResponse.Item> findNotifications(Long memberId, String cursor, Integer pageSize);
}
