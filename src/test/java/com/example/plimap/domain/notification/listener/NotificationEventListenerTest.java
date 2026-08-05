package com.example.plimap.domain.notification.listener;

import com.example.plimap.domain.member.event.MemberFollowedEvent;
import com.example.plimap.domain.member.event.MemberWithdrawnEvent;
import com.example.plimap.domain.notification.service.command.NotificationCommandService;
import com.example.plimap.domain.pin.event.PinCreatedEvent;
import com.example.plimap.domain.pin.event.PinLikedEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationEventListenerTest {

    private final NotificationCommandService notificationCommandService = mock(NotificationCommandService.class);
    private final NotificationEventListener listener = new NotificationEventListener(notificationCommandService);

    @Test
    void 팔로우_이벤트를_받으면_팔로우_대상에게_알림을_생성한다() {
        // given
        MemberFollowedEvent event = new MemberFollowedEvent(1L, 2L);

        // when
        listener.handleMemberFollowed(event);

        // then
        verify(notificationCommandService).createFollowNotification(2L, 1L);
    }

    @Test
    void 핀_등록_이벤트를_받으면_팔로워_알림_생성을_요청한다() {
        // given
        PinCreatedEvent event = new PinCreatedEvent(100L, 1L);

        // when
        listener.handlePinCreated(event);

        // then
        verify(notificationCommandService).createPinCreatedNotifications(100L, 1L);
    }

    @Test
    void 좋아요_이벤트를_받으면_핀_주인에게_알림을_생성한다() {
        // given
        PinLikedEvent event = new PinLikedEvent(100L, 1L, 2L);

        // when
        listener.handlePinLiked(event);

        // then
        verify(notificationCommandService).createPinLikedNotification(1L, 2L, 100L);
    }

    @Test
    void 회원_탈퇴_이벤트를_받으면_해당_회원의_알림을_전부_지운다() {
        // given
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(1L, null);

        // when
        listener.handleMemberWithdrawn(event);

        // then
        verify(notificationCommandService).deleteByMemberId(1L);
    }
}
