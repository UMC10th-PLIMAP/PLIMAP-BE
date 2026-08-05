package com.example.plimap.domain.notification.listener;

import com.example.plimap.domain.member.event.MemberFollowedEvent;
import com.example.plimap.domain.member.event.MemberWithdrawnEvent;
import com.example.plimap.domain.notification.service.command.NotificationCommandService;
import com.example.plimap.domain.pin.event.PinCreatedEvent;
import com.example.plimap.domain.pin.event.PinLikedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationCommandService notificationCommandService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberFollowed(MemberFollowedEvent event) {
        notificationCommandService.createFollowNotification(event.followingId(), event.followerId());
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePinCreated(PinCreatedEvent event) {
        notificationCommandService.createPinCreatedNotifications(event.pinId(), event.authorId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePinLiked(PinLikedEvent event) {
        notificationCommandService.createPinLikedNotification(event.pinOwnerId(), event.likerId(), event.pinId());
    }

    // 자발적 탈퇴/벌점 자동 탈퇴 모두 MemberWithdrawnEvent를 발행하므로 여기서 공통으로 처리한다.
    // 자동 탈퇴는 AdminCommandServiceImpl의 캐스케이드에서 핀 하드삭제 전에 이미 동기적으로
    // 지우므로 여기선 남는 게 없어 실질적으로 자발적 탈퇴 케이스만 지운다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberWithdrawn(MemberWithdrawnEvent event) {
        notificationCommandService.deleteByMemberId(event.memberId());
    }
}
