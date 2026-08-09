package com.example.plimap.domain.notification.service.command.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.domain.notification.converter.NotificationConverter;
import com.example.plimap.domain.notification.dto.response.NotificationResDTO;
import com.example.plimap.domain.notification.entity.Notification;
import com.example.plimap.domain.notification.enums.NotificationType;
import com.example.plimap.domain.notification.repository.NotificationRepository;
import com.example.plimap.domain.notification.service.command.NotificationCommandService;
import com.example.plimap.domain.notification.sse.NotificationEmitterRegistry;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.global.external.storage.ProfileImageStorage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 알림 생성은 {@code @TransactionalEventListener(phase = AFTER_COMMIT)}에서 호출되므로,
 * 원본 트랜잭션이 이미 커밋된 뒤에도 독립적으로 커밋되도록 항상 REQUIRES_NEW로 실행한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class NotificationCommandServiceImpl implements NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final MemberQueryService memberQueryService;
    private final PinQueryService pinQueryService;
    private final NotificationEmitterRegistry notificationEmitterRegistry;
    private final ProfileImageStorage profileImageStorage;

    @Override
    public void createFollowNotification(Long recipientId, Long actorId) {
        if (recipientId.equals(actorId)) {
            return;
        }

        Member recipient = memberQueryService.getActiveMember(recipientId);
        Member actor = memberQueryService.getActiveMember(actorId);

        Notification saved = notificationRepository.save(
                Notification.create(recipient, actor, null, NotificationType.FOLLOW));
        pushAfterCommit(saved);
    }

    @Override
    public void createPinCreatedNotifications(Long pinId, Long authorId) {
        Pin pin = pinQueryService.getActivePin(pinId);
        Member author = memberQueryService.getActiveMember(authorId);
        List<Member> followers = memberQueryService.findAllFollowers(authorId);

        List<Notification> notifications = followers.stream()
                .filter(follower -> !follower.getId().equals(authorId))
                .map(follower -> Notification.create(follower, author, pin, NotificationType.PIN_CREATED))
                .toList();

        List<Notification> saved = notificationRepository.saveAll(notifications);
        saved.forEach(this::pushAfterCommit);
    }

    @Override
    public void createPinLikedNotification(Long recipientId, Long actorId, Long pinId) {
        if (recipientId.equals(actorId)) {
            return;
        }

        Member recipient = memberQueryService.getActiveMember(recipientId);
        Member actor = memberQueryService.getActiveMember(actorId);
        Pin pin = pinQueryService.getActivePin(pinId);

        Notification saved = notificationRepository.save(
                Notification.create(recipient, actor, pin, NotificationType.PIN_LIKED));
        pushAfterCommit(saved);
    }

    @Override
    @Transactional // 클래스 레벨 REQUIRES_NEW를 오버라이드: 자동탈퇴 캐스케이드의 일부로 호출자 트랜잭션에 참여해야
                   // 이후 단계(핀 하드삭제)가 실패했을 때 이 삭제도 함께 롤백된다. MemberWithdrawnEvent
                   // 리스너(AFTER_COMMIT)에서 호출될 때는 진행 중인 트랜잭션이 없어 새로 시작된다.
    public void deleteByMemberId(Long memberId) {
        notificationRepository.deleteByMemberId(memberId);
    }

    private void pushAfterCommit(Notification notification) {
        Long recipientId = notification.getRecipient().getId();

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notificationEmitterRegistry.sendToMember(
                    recipientId, "notification", toItem(notification));
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationEmitterRegistry.sendToMember(
                        recipientId, "notification", toItem(notification));
            }
        });
    }

    private NotificationResDTO.Item toItem(Notification notification) {
        String actorProfileImageUrl =
                profileImageStorage.getPublicUrlOrNull(notification.getActor().getProfileImageObjectKey());
        return NotificationConverter.toItem(notification, actorProfileImageUrl);
    }
}
