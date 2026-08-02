package com.example.plimap.domain.notification.converter;

import com.example.plimap.domain.notification.dto.Pagination;
import com.example.plimap.domain.notification.dto.response.NotificationResDTO;
import com.example.plimap.domain.notification.entity.Notification;
import java.util.List;

public class NotificationConverter {

    private NotificationConverter() {}

    public static NotificationResDTO.Item toItem(Notification notification) {
        return new NotificationResDTO.Item(
                notification.getId(),
                notification.getType(),
                notification.getActor().getId(),
                notification.getActor().getDisplayNickname(),
                notification.getActor().getProfileImageObjectKey(),
                notification.getPin() != null ? notification.getPin().getId() : null,
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

    public static <T> Pagination<T> toPagination(
            List<T> data,
            String nextCursor,
            Boolean hasNext,
            Integer pageSize
    ) {
        return Pagination.<T>builder()
                .data(data)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .pageSize(pageSize)
                .build();
    }
}
