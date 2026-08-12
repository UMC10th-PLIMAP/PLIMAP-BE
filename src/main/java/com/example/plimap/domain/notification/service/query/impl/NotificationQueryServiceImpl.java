package com.example.plimap.domain.notification.service.query.impl;

import com.example.plimap.domain.notification.converter.NotificationConverter;
import com.example.plimap.domain.notification.dto.Pagination;
import com.example.plimap.domain.notification.dto.response.NotificationResponse;
import com.example.plimap.domain.notification.repository.query.NotificationQueryRepository;
import com.example.plimap.domain.notification.repository.query.NotificationRow;
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
    public Pagination<NotificationResponse.Item> findNotifications(Long memberId, String cursor, Integer pageSize) {
        Pagination<NotificationRow> notifications = notificationQueryRepository.findNotifications(memberId, cursor, pageSize);

        List<NotificationResponse.Item> data = notifications.data().stream()
                .map(row -> NotificationConverter.toItem(
                        row.notification(),
                        profileImageStorage.getPublicUrlOrNull(row.notification().getActor().getProfileImageObjectKey()),
                        row.isFollowing(),
                        row.isFollowingViewer()
                ))
                .toList();

        return NotificationConverter.toPagination(
                data, notifications.nextCursor(), notifications.hasNext(), notifications.pageSize());
    }
}
