package com.example.plimap.domain.notification.listener;

import com.example.plimap.domain.member.event.MemberFollowedEvent;
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
}
