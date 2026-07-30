package com.example.plimap.domain.notification.repository.query;

import com.example.plimap.domain.notification.dto.Pagination;
import com.example.plimap.domain.notification.dto.response.NotificationResDTO;

public interface NotificationQueryRepository {

    Pagination<NotificationResDTO.Item> findNotifications(Long memberId, String cursor, Integer pageSize);
}
