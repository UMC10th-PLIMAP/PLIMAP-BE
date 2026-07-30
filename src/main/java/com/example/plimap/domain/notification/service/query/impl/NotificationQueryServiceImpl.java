package com.example.plimap.domain.notification.service.query.impl;

import com.example.plimap.domain.notification.dto.Pagination;
import com.example.plimap.domain.notification.dto.response.NotificationResDTO;
import com.example.plimap.domain.notification.repository.query.NotificationQueryRepository;
import com.example.plimap.domain.notification.service.query.NotificationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final NotificationQueryRepository notificationQueryRepository;

    @Override
    public Pagination<NotificationResDTO.Item> findNotifications(Long memberId, String cursor, Integer pageSize) {
        return notificationQueryRepository.findNotifications(memberId, cursor, pageSize);
    }
}
