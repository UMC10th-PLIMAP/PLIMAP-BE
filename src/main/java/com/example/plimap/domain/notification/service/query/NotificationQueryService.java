package com.example.plimap.domain.notification.service.query;

import com.example.plimap.domain.notification.dto.Pagination;
import com.example.plimap.domain.notification.dto.response.NotificationResDTO;

public interface NotificationQueryService {

    Pagination<NotificationResDTO.Item> findNotifications(Long memberId, String cursor, Integer pageSize);
}
