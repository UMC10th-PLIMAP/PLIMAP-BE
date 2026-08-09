package com.example.plimap.domain.notification.service.command.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.domain.notification.entity.Notification;
import com.example.plimap.domain.notification.enums.NotificationType;
import com.example.plimap.domain.notification.repository.NotificationRepository;
import com.example.plimap.domain.notification.sse.NotificationEmitterRegistry;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.global.external.storage.ProfileImageStorage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationCommandServiceImplTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final MemberQueryService memberQueryService = mock(MemberQueryService.class);
    private final PinQueryService pinQueryService = mock(PinQueryService.class);
    private final NotificationEmitterRegistry notificationEmitterRegistry = mock(NotificationEmitterRegistry.class);
    private final ProfileImageStorage profileImageStorage = mock(ProfileImageStorage.class);
    private final NotificationCommandServiceImpl notificationCommandService = new NotificationCommandServiceImpl(
            notificationRepository, memberQueryService, pinQueryService, notificationEmitterRegistry, profileImageStorage);

    @Test
    void 팔로우_알림을_생성하고_실시간으로_푸시한다() {
        // given
        Member recipient = mock(Member.class);
        Member actor = mock(Member.class);
        when(recipient.getId()).thenReturn(1L);
        when(memberQueryService.getActiveMember(1L)).thenReturn(recipient);
        when(memberQueryService.getActiveMember(2L)).thenReturn(actor);
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        notificationCommandService.createFollowNotification(1L, 2L);

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.FOLLOW);
        assertThat(captor.getValue().getRecipient()).isSameAs(recipient);
        assertThat(captor.getValue().getActor()).isSameAs(actor);
        assertThat(captor.getValue().getPin()).isNull();
        verify(notificationEmitterRegistry).sendToMember(eq(1L), eq("notification"), any());
    }

    @Test
    void 자기_자신을_대상으로_한_팔로우_알림은_생성하지_않는다() {
        // when
        notificationCommandService.createFollowNotification(1L, 1L);

        // then
        verify(notificationRepository, never()).save(any());
        verify(notificationEmitterRegistry, never()).sendToMember(any(), anyString(), any());
    }

    @Test
    void 핀_등록_알림을_팔로워_전원에게_생성하고_각각_푸시한다() {
        // given
        Pin pin = mock(Pin.class);
        Member author = mock(Member.class);
        Member follower1 = mock(Member.class);
        Member follower2 = mock(Member.class);
        when(follower1.getId()).thenReturn(10L);
        when(follower2.getId()).thenReturn(20L);
        when(pinQueryService.getActivePin(100L)).thenReturn(pin);
        when(memberQueryService.getActiveMember(1L)).thenReturn(author);
        when(memberQueryService.findAllFollowers(1L)).thenReturn(List.of(follower1, follower2));
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        notificationCommandService.createPinCreatedNotifications(100L, 1L);

        // then
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue())
                .allSatisfy(notification -> {
                    assertThat(notification.getType()).isEqualTo(NotificationType.PIN_CREATED);
                    assertThat(notification.getActor()).isSameAs(author);
                    assertThat(notification.getPin()).isSameAs(pin);
                });
        verify(notificationEmitterRegistry).sendToMember(eq(10L), eq("notification"), any());
        verify(notificationEmitterRegistry).sendToMember(eq(20L), eq("notification"), any());
    }

    @Test
    void 좋아요_알림을_생성하고_실시간으로_푸시한다() {
        // given
        Pin pin = mock(Pin.class);
        Member recipient = mock(Member.class);
        Member actor = mock(Member.class);
        when(recipient.getId()).thenReturn(1L);
        when(pinQueryService.getActivePin(100L)).thenReturn(pin);
        when(memberQueryService.getActiveMember(1L)).thenReturn(recipient);
        when(memberQueryService.getActiveMember(2L)).thenReturn(actor);
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        notificationCommandService.createPinLikedNotification(1L, 2L, 100L);

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.PIN_LIKED);
        assertThat(captor.getValue().getRecipient()).isSameAs(recipient);
        assertThat(captor.getValue().getActor()).isSameAs(actor);
        assertThat(captor.getValue().getPin()).isSameAs(pin);
        verify(notificationEmitterRegistry).sendToMember(eq(1L), eq("notification"), any());
    }

    @Test
    void 자신의_핀에_스스로_좋아요_눌러도_알림을_생성하지_않는다() {
        // when
        notificationCommandService.createPinLikedNotification(1L, 1L, 100L);

        // then
        verify(notificationRepository, never()).save(any());
        verify(notificationEmitterRegistry, never()).sendToMember(any(), anyString(), any());
    }

    @Test
    void 회원_기준으로_알림을_전부_삭제한다() {
        // when
        notificationCommandService.deleteByMemberId(1L);

        // then
        verify(notificationRepository).deleteByMemberId(1L);
    }
}
