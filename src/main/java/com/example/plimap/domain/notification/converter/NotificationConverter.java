package com.example.plimap.domain.notification.converter;

import com.example.plimap.domain.notification.dto.Pagination;
import com.example.plimap.domain.notification.dto.response.NotificationResponse;
import com.example.plimap.domain.notification.entity.Notification;
import com.example.plimap.domain.pin.entity.Pin;
import java.util.List;

public class NotificationConverter {

    private NotificationConverter() {}

    public static NotificationResponse.Item toItem(
            Notification notification,
            String actorProfileImageUrl,
            boolean isFollowing,
            boolean isFollowingViewer
    ) {
        Pin pin = notification.getPin();
        return new NotificationResponse.Item(
                notification.getId(),
                notification.getType(),
                notification.getActor().getId(),
                notification.getActor().getDisplayNickname(),
                actorProfileImageUrl,
                isFollowing,
                isFollowingViewer,
                pin != null ? pin.getId() : null,
                pin != null ? pin.getPlace().getName() : null,
                pin != null ? pin.getPlaceTrack().getTrack().getAlbumImageUrl() : null,
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
