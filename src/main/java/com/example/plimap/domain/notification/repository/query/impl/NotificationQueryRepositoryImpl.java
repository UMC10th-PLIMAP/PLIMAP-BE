package com.example.plimap.domain.notification.repository.query.impl;

import com.example.plimap.domain.member.entity.QMember;
import com.example.plimap.domain.notification.converter.NotificationConverter;
import com.example.plimap.domain.notification.dto.Pagination;
import com.example.plimap.domain.notification.entity.Notification;
import com.example.plimap.domain.notification.entity.QNotification;
import com.example.plimap.domain.notification.exception.NotificationErrorCode;
import com.example.plimap.domain.notification.exception.NotificationException;
import com.example.plimap.domain.notification.repository.query.NotificationQueryRepository;
import com.example.plimap.domain.pin.entity.QPin;
import com.querydsl.core.types.dsl.BooleanExpression;
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

    @Override
    public Pagination<Notification> findNotifications(Long memberId, String cursor, Integer pageSize) {
        Cursor parsedCursor = parseCursor(cursor);

        List<Notification> notifications = queryFactory
                .selectFrom(notification)
                .join(notification.actor, member).fetchJoin()
                .leftJoin(notification.pin, pin).fetchJoin()
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

        Notification last = notifications.getLast();
        String nextCursor = hasNext
                ? last.getCreatedAt() + "/" + last.getId()
                : null;

        return NotificationConverter.toPagination(notifications, nextCursor, hasNext, pageSize);
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
