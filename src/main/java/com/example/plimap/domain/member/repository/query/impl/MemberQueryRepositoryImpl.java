package com.example.plimap.domain.member.repository.query.impl;

import com.example.plimap.domain.member.converter.MemberConverter;
import com.example.plimap.domain.member.dto.CursorInfo;
import com.example.plimap.domain.member.dto.Pagination;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.entity.QMember;
import com.example.plimap.domain.member.entity.QMemberFollow;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.repository.query.MemberQueryRepository;
import com.example.plimap.domain.report.entity.QReport;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberQueryRepositoryImpl implements MemberQueryRepository {

    private static final int REPORT_HIDE_THRESHOLD = 10;

    private final JPAQueryFactory queryFactory;

    @Override
    public Pagination<MemberResDTO.FollowerItem> findFollowersByMemberId(Long viewerId, Long memberId, String cursor, Integer pageSize) {
        QMemberFollow memberFollow = QMemberFollow.memberFollow;
        QMember follower = QMember.member;
        CursorInfo cursorInfo = parseCursor(cursor);

        List<MemberResDTO.FollowerItem> data = queryFactory
                .select(
                        Projections.constructor(
                                MemberResDTO.FollowerItem.class,
                                follower.id,
                                follower.nickname,
                                follower.name,
                                follower.profileImageObjectKey,
                                memberFollow.createdAt,
                                isFollowedByViewer(viewerId, follower.id)
                        )
                )
                .from(memberFollow)
                .join(memberFollow.follower, follower)
                .where(
                        memberFollow.following.id.eq(memberId),
                        follower.status.eq(MemberStatus.ACTIVE),
                        follower.deletedAt.isNull(),
                        follower.reportCount.lt(REPORT_HIDE_THRESHOLD),
                        notReportedByViewer(viewerId, follower.id),
                        cursorCondition(memberFollow, follower.id, cursorInfo.createdAt(), cursorInfo.memberId())
                )
                .orderBy(memberFollow.createdAt.desc(), follower.id.desc())
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = data.size() > pageSize;
        if (hasNext) {
            data.remove(pageSize.intValue());
        }

        if (data.isEmpty()) {
            return MemberConverter.toPagination(data, null, false, pageSize);
        }

        MemberResDTO.FollowerItem last = data.get(data.size() - 1);
        String nextCursor = hasNext
                ? last.followedAt() + "/" + last.id()
                : null;

        return MemberConverter.toPagination(data, nextCursor, hasNext, pageSize);
    }

    @Override
    public Pagination<MemberResDTO.FollowingItem> findFollowingByMemberId(Long viewerId, Long memberId, String cursor, Integer pageSize) {
        QMemberFollow memberFollow = QMemberFollow.memberFollow;
        QMember following = QMember.member;
        CursorInfo cursorInfo = parseCursor(cursor);

        List<MemberResDTO.FollowingItem> data = queryFactory
                .select(
                        Projections.constructor(
                                MemberResDTO.FollowingItem.class,
                                following.id,
                                following.nickname,
                                following.name,
                                following.profileImageObjectKey,
                                memberFollow.createdAt,
                                isFollowedByViewer(viewerId, following.id)
                        )
                )
                .from(memberFollow)
                .join(memberFollow.following, following)
                .where(
                        memberFollow.follower.id.eq(memberId),
                        following.status.eq(MemberStatus.ACTIVE),
                        following.deletedAt.isNull(),
                        following.reportCount.lt(REPORT_HIDE_THRESHOLD),
                        notReportedByViewer(viewerId, following.id),
                        cursorCondition(memberFollow, following.id, cursorInfo.createdAt(), cursorInfo.memberId())
                )
                .orderBy(memberFollow.createdAt.desc(), following.id.desc())
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = data.size() > pageSize;
        if (hasNext) {
            data.remove(pageSize.intValue());
        }

        if (data.isEmpty()) {
            return MemberConverter.toPagination(data, null, false, pageSize);
        }

        MemberResDTO.FollowingItem last = data.get(data.size() - 1);
        String nextCursor = hasNext
                ? last.followedAt() + "/" + last.id()
                : null;

        return MemberConverter.toPagination(data, nextCursor, hasNext, pageSize);
    }

    @Override
    public Optional<Member> findVisibleActiveMember(Long targetMemberId, Long viewerId) {
        QMember target = QMember.member;

        return Optional.ofNullable(queryFactory
                .selectFrom(target)
                .where(
                        target.id.eq(targetMemberId),
                        target.status.eq(MemberStatus.ACTIVE),
                        target.deletedAt.isNull(),
                        target.reportCount.lt(REPORT_HIDE_THRESHOLD),
                        notReportedByViewer(viewerId, target.id)
                )
                .fetchOne()
        );
    }

    private BooleanExpression notReportedByViewer(Long viewerId, NumberPath<Long> targetId) {
        if (viewerId == null) {
            return null;
        }
        QReport report = QReport.report;
        return JPAExpressions
                .selectOne()
                .from(report)
                .where(report.reportedMember.id.eq(targetId), report.reporter.id.eq(viewerId))
                .notExists();
    }

    private CursorInfo parseCursor(String cursor) {
        if (cursor == null) {
            return new CursorInfo(null, null);
        }
        try {
            String[] parts = cursor.split("/");
            if (parts.length != 2) {
                throw new MemberException(MemberErrorCode.INVALID_CURSOR);
            }

            Instant createdAt = Instant.parse(parts[0]);
            long memberId = Long.parseLong(parts[1]);

            return new CursorInfo(createdAt, memberId);
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new MemberException(MemberErrorCode.INVALID_CURSOR, e);
        }
    }

    private BooleanExpression isFollowedByViewer(Long viewerId, NumberPath<Long> targetId) {
        QMemberFollow viewerFollow = new QMemberFollow("viewerFollow");
        return JPAExpressions
                .selectOne()
                .from(viewerFollow)
                .where(
                        viewerFollow.follower.id.eq(viewerId),
                        viewerFollow.following.id.eq(targetId)
                )
                .exists();
    }

    private BooleanExpression cursorCondition(
            QMemberFollow memberFollow,
            NumberPath<Long> counterpartId,
            Instant createdAt,
            Long memberId
    ) {
        if (createdAt == null || memberId == null) {
            return null;
        }

        return memberFollow.createdAt.lt(createdAt)
                .or(
                        memberFollow.createdAt.eq(createdAt)
                                .and(counterpartId.lt(memberId))
                );
    }
}
