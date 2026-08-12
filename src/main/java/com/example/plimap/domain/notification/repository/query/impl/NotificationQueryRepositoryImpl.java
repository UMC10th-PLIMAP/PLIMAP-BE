package com.example.plimap.domain.notification.repository.query.impl;

import com.example.plimap.domain.member.entity.QMember;
import com.example.plimap.domain.member.entity.QMemberFollow;
import com.example.plimap.domain.notification.converter.NotificationConverter;
import com.example.plimap.domain.notification.dto.Pagination;
import com.example.plimap.domain.notification.entity.QNotification;
import com.example.plimap.domain.notification.exception.NotificationErrorCode;
import com.example.plimap.domain.notification.exception.NotificationException;
import com.example.plimap.domain.notification.repository.query.NotificationQueryRepository;
import com.example.plimap.domain.notification.repository.query.NotificationRow;
import com.example.plimap.domain.pin.entity.QPin;
import com.example.plimap.domain.place.entity.QPlace;
import com.example.plimap.domain.track.entity.QPlaceTrack;
import com.example.plimap.domain.track.entity.QTrack;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationQueryRepositoryImpl implements NotificationQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QNotification notification = QNotification.notification;
    private final QMember member = QMember.member;
    private final QPin pin = QPin.pin;
    private final QPlace place = QPlace.place;
    private final QPlaceTrack placeTrack = QPlaceTrack.placeTrack;
    private final QTrack track = QTrack.track;

    @Override
    public Pagination<NotificationRow> findNotifications(Long memberId, String cursor, Integer pageSize) {
        Cursor parsedCursor = parseCursor(cursor);

        List<NotificationRow> notifications = queryFactory
                .select(Projections.constructor(
                        NotificationRow.class,
                        notification,
                        isFollowedByRecipient(memberId, notification.actor.id),
                        isRecipientFollowedByActor(memberId, notification.actor.id)
                ))
                .from(notification)
                .join(notification.actor, member).fetchJoin()
                .leftJoin(notification.pin, pin).fetchJoin()
                .leftJoin(pin.place, place).fetchJoin()
                .leftJoin(pin.placeTrack, placeTrack).fetchJoin()
                .leftJoin(placeTrack.track, track).fetchJoin()
                .where(
                        notification.recipient.id.eq(memberId),
                        cursorCondition(parsedCursor)
                )
                .orderBy(notification.createdAt.desc(), notification.id.desc())
                .limit(pageSize + 1)
                .fetch();

        if (notifications.isEmpty()) {
            return NotificationConverter.toPagination(List.of(), null, false, pageSize);
        }

        boolean hasNext = notifications.size() > pageSize;
        if (hasNext) {
            notifications = notifications.subList(0, pageSize);
        }

        NotificationRow last = notifications.getLast();
        String nextCursor = hasNext
                ? last.notification().getCreatedAt() + "/" + last.notification().getId()
                : null;

        return NotificationConverter.toPagination(notifications, nextCursor, hasNext, pageSize);
    }

    // 수신자 -> 알림 발신자 방향. FOLLOW 알림에서 "내가 이 사람을 맞팔로우하는지" 버튼 상태에 사용.
    private BooleanExpression isFollowedByRecipient(Long recipientId, NumberPath<Long> actorId) {
        QMemberFollow recipientFollow = new QMemberFollow("recipientFollow");
        return JPAExpressions
                .selectOne()
                .from(recipientFollow)
                .where(
                        recipientFollow.follower.id.eq(recipientId),
                        recipientFollow.following.id.eq(actorId)
                )
                .exists();
    }

    // 알림 발신자 -> 수신자 방향(역방향). FOLLOW 알림 발생 시점엔 항상 true였지만, 그 뒤 발신자가
    // 언팔로우했을 수 있어 조회 시점 값을 다시 계산한다.
    private BooleanExpression isRecipientFollowedByActor(Long recipientId, NumberPath<Long> actorId) {
        QMemberFollow actorFollow = new QMemberFollow("actorFollow");
        return JPAExpressions
                .selectOne()
                .from(actorFollow)
                .where(
                        actorFollow.follower.id.eq(actorId),
                        actorFollow.following.id.eq(recipientId)
                )
                .exists();
    }

    private Cursor parseCursor(String cursor) {
        if (cursor == null) {
            return new Cursor(null, null);
        }

        try {
            String[] parts = cursor.split("/", -1);
            if (parts.length != 2) {
                throw new NotificationException(NotificationErrorCode.INVALID_CURSOR);
            }

            Instant createdAt = Instant.parse(parts[0]);
            long id = Long.parseLong(parts[1]);
            if (id <= 0) {
                throw new NotificationException(NotificationErrorCode.INVALID_CURSOR);
            }

            return new Cursor(createdAt, id);
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new NotificationException(NotificationErrorCode.INVALID_CURSOR);
        }
    }

    private BooleanExpression cursorCondition(Cursor cursor) {
        if (cursor.createdAt() == null || cursor.id() == null) {
            return null;
        }

        return notification.createdAt.lt(cursor.createdAt())
                .or(
                        notification.createdAt.eq(cursor.createdAt())
                                .and(notification.id.lt(cursor.id()))
                );
    }

    private record Cursor(Instant createdAt, Long id) {}
}
