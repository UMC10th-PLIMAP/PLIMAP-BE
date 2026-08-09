package com.example.plimap.domain.notification.service.query.impl;

import com.example.plimap.domain.notification.converter.NotificationConverter;
import com.example.plimap.domain.notification.dto.Pagination;
import com.example.plimap.domain.notification.dto.response.NotificationResDTO;
import com.example.plimap.domain.notification.entity.Notification;
import com.example.plimap.domain.notification.repository.query.NotificationQueryRepository;
import com.example.plimap.domain.notification.service.query.NotificationQueryService;
import com.example.plimap.global.external.storage.ProfileImageStorage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final NotificationQueryRepository notificationQueryRepository;
    private final ProfileImageStorage profileImageStorage;

    @Override
    public Pagination<NotificationResDTO.Item> findNotifications(Long memberId, String cursor, Integer pageSize) {
        Pagination<Notification> notifications = notificationQueryRepository.findNotifications(memberId, cursor, pageSize);

        List<NotificationResDTO.Item> data = notifications.data().stream()
                .map(notification -> NotificationConverter.toItem(
                        notification,
                        profileImageStorage.getPublicUrlOrNull(notification.getActor().getProfileImageObjectKey())
                ))
                .toList();

        return NotificationConverter.toPagination(
                data, notifications.nextCursor(), notifications.hasNext(), notifications.pageSize());
    }
}
