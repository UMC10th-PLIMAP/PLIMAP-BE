package com.example.plimap.domain.member.repository.query.impl;

import com.example.plimap.domain.member.converter.MemberConverter;
import com.example.plimap.domain.member.dto.CursorInfo;
import com.example.plimap.domain.member.dto.MemberSearchCursorInfo;
import com.example.plimap.domain.member.dto.Pagination;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.entity.QMember;
import com.example.plimap.domain.member.entity.QMemberFollow;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.repository.query.MemberFollowRow;
import com.example.plimap.domain.member.repository.query.MemberQueryRepository;
import com.example.plimap.domain.member.repository.query.MemberSearchRow;
import com.example.plimap.domain.auth.entity.QSocialAccount;
import com.example.plimap.domain.report.entity.QReport;
import com.example.plimap.global.apiPayload.code.GeneralErrorCode;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    public Pagination<MemberFollowRow> findFollowersByMemberId(Long viewerId, Long memberId, String cursor, Integer pageSize) {
        QMemberFollow memberFollow = QMemberFollow.memberFollow;
        QMember follower = QMember.member;
        CursorInfo cursorInfo = parseCursor(cursor);

        List<MemberFollowRow> data = queryFactory
                .select(
                        Projections.constructor(
                                MemberFollowRow.class,
                                follower.id,
                                follower.nickname,
                                follower.name,
                                follower.profileImageObjectKey,
                                memberFollow.createdAt,
                                isFollowedByViewer(viewerId, follower.id),
                                isViewerFollowedByTarget(viewerId, follower.id)
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

        MemberFollowRow last = data.get(data.size() - 1);
        String nextCursor = hasNext
                ? last.followedAt() + "/" + last.id()
                : null;

        return MemberConverter.toPagination(data, nextCursor, hasNext, pageSize);
    }

    @Override
    public Pagination<MemberFollowRow> findFollowingByMemberId(Long viewerId, Long memberId, String cursor, Integer pageSize) {
        QMemberFollow memberFollow = QMemberFollow.memberFollow;
        QMember following = QMember.member;
        CursorInfo cursorInfo = parseCursor(cursor);

        List<MemberFollowRow> data = queryFactory
                .select(
                        Projections.constructor(
                                MemberFollowRow.class,
                                following.id,
                                following.nickname,
                                following.name,
                                following.profileImageObjectKey,
                                memberFollow.createdAt,
                                isFollowedByViewer(viewerId, following.id),
                                isViewerFollowedByTarget(viewerId, following.id)
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

        MemberFollowRow last = data.get(data.size() - 1);
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

    @Override
    public Page<Member> searchMembers(String query, MemberStatus status, Pageable pageable) {
        QMember target = QMember.member;
        BooleanExpression statusCondition = status != null ? target.status.eq(status) : null;
        BooleanExpression queryCondition = searchQueryCondition(target, query);

        List<Member> content = queryFactory
                .selectFrom(target)
                .where(statusCondition, queryCondition)
                .orderBy(target.createdAt.desc(), target.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = Optional.ofNullable(
                queryFactory
                        .select(target.count())
                        .from(target)
                        .where(statusCondition, queryCondition)
                        .fetchOne()
        ).orElse(0L);

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Pagination<MemberSearchRow> searchActiveMembers(Long viewerId, String keyword, String cursor, Integer pageSize) {
        QMember target = QMember.member;
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        NumberExpression<Integer> followingGroupScore = followingGroupScoreExpression(viewerId, target.id);
        NumberExpression<Integer> nicknameScore = matchScoreExpression(target.nickname, keyword);
        NumberExpression<Integer> nameScore = matchScoreExpression(target.name, keyword);
        MemberSearchCursorInfo cursorInfo = parseSearchCursor(cursor);

        List<MemberSearchRow> data = queryFactory
                .select(
                        Projections.constructor(
                                MemberSearchRow.class,
                                target.id,
                                target.nickname,
                                target.name,
                                target.profileImageObjectKey,
                                target.createdAt,
                                isFollowedByViewer(viewerId, target.id),
                                isViewerFollowedByTarget(viewerId, target.id),
                                nicknameScore,
                                nameScore
                        )
                )
                .from(target)
                .where(
                        target.status.eq(MemberStatus.ACTIVE),
                        target.deletedAt.isNull(),
                        target.reportCount.lt(REPORT_HIDE_THRESHOLD),
                        notReportedByViewer(viewerId, target.id),
                        target.id.ne(viewerId),
                        hasKeyword ? nicknameScore.gt(0).or(nameScore.gt(0)) : null,
                        searchCursorCondition(followingGroupScore, nicknameScore, nameScore, target.createdAt, target.id, cursorInfo)
                )
                .orderBy(
                        followingGroupScore.asc(),
                        nicknameScore.desc(),
                        nameScore.desc(),
                        target.createdAt.desc(),
                        target.id.desc()
                )
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = data.size() > pageSize;
        if (hasNext) {
            data.remove(pageSize.intValue());
        }

        if (data.isEmpty()) {
            return MemberConverter.toPagination(data, null, false, pageSize);
        }

        MemberSearchRow last = data.get(data.size() - 1);
        String nextCursor = hasNext ? encodeSearchCursor(last) : null;

        return MemberConverter.toPagination(data, nextCursor, hasNext, pageSize);
    }

    private BooleanExpression searchQueryCondition(QMember target, String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        QSocialAccount socialAccount = QSocialAccount.socialAccount;
        BooleanExpression emailMatches = JPAExpressions
                .selectOne()
                .from(socialAccount)
                .where(socialAccount.member.id.eq(target.id), socialAccount.email.containsIgnoreCase(query))
                .exists();
        return target.nickname.containsIgnoreCase(query)
                .or(target.name.containsIgnoreCase(query))
                .or(emailMatches);
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
                throw new MemberException(GeneralErrorCode.INVALID_CURSOR);
            }

            Instant createdAt = Instant.parse(parts[0]);
            long memberId = Long.parseLong(parts[1]);

            return new CursorInfo(createdAt, memberId);
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new MemberException(GeneralErrorCode.INVALID_CURSOR, e);
        }
    }

    // 뷰어 -> targetId 방향. 목록 대상(memberId)과 무관하게 "내가 이 사람을 팔로우하는지"만 나타내므로,
    // 단독으로는 맞팔 여부를 의미하지 않는다(내 목록을 내가 볼 때만 우연히 일치).
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

    // targetId -> 뷰어 방향(역방향). isFollowedByViewer와 함께 봐야 실제 맞팔(양방향) 여부를 판단할 수 있다.
    private BooleanExpression isViewerFollowedByTarget(Long viewerId, NumberPath<Long> targetId) {
        QMemberFollow targetFollow = new QMemberFollow("targetFollow");
        return JPAExpressions
                .selectOne()
                .from(targetFollow)
                .where(
                        targetFollow.follower.id.eq(targetId),
                        targetFollow.following.id.eq(viewerId)
                )
                .exists();
    }

    // 친구 찾기 검색 1순위 정렬 기준: 뷰어가 아직 팔로우하지 않은 회원(0)을 팔로우 중인 회원(1, 맞팔 포함)보다 앞에 노출한다.
    private NumberExpression<Integer> followingGroupScoreExpression(Long viewerId, NumberPath<Long> targetId) {
        return new CaseBuilder()
                .when(isFollowedByViewer(viewerId, targetId))
                .then(1)
                .otherwise(0);
    }

    // 닉네임/이름 공용 연관성 점수: 검색어로 시작(2) > 검색어를 포함(1) > 불일치(0).
    // keyword가 비어있으면 매칭을 따지지 않고 전체 노출해야 하므로 실제 CASE 없이 상수 0을 반환한다.
    // field가 null(이름 없음)인 경우 startsWith/containsIgnoreCase는 SQL상 UNKNOWN→false로 평가되어
    // 자연스럽게 0점(불일치와 동일)이 되므로 별도 null 분기가 필요 없다.
    private NumberExpression<Integer> matchScoreExpression(StringPath field, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Expressions.asNumber(0);
        }
        return new CaseBuilder()
                .when(field.startsWithIgnoreCase(keyword)).then(2)
                .when(field.containsIgnoreCase(keyword)).then(1)
                .otherwise(0);
    }

    private MemberSearchCursorInfo parseSearchCursor(String cursor) {
        if (cursor == null) {
            return new MemberSearchCursorInfo(null, null, null, null, null);
        }
        try {
            String[] parts = cursor.split("/");
            if (parts.length != 5) {
                throw new MemberException(GeneralErrorCode.INVALID_CURSOR);
            }

            Integer followingGroupScore = Integer.parseInt(parts[0]);
            Integer nicknameScore = Integer.parseInt(parts[1]);
            Integer nameScore = Integer.parseInt(parts[2]);
            Instant createdAt = Instant.parse(parts[3]);
            Long id = Long.parseLong(parts[4]);

            return new MemberSearchCursorInfo(followingGroupScore, nicknameScore, nameScore, createdAt, id);
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new MemberException(GeneralErrorCode.INVALID_CURSOR, e);
        }
    }

    // findFollowers/findFollowing의 2단 cursorCondition을 5단(팔로우 그룹 + 닉네임 점수 + 이름 점수 + 가입일 + id)으로
    // 확장한 keyset 비교. 팔로우 그룹만 오름차순(gt)이고 나머지는 모두 내림차순(lt)이라 부등호 방향이 다르다.
    private BooleanExpression searchCursorCondition(
            NumberExpression<Integer> followingGroupScore,
            NumberExpression<Integer> nicknameScore,
            NumberExpression<Integer> nameScore,
            DateTimePath<Instant> createdAt,
            NumberPath<Long> id,
            MemberSearchCursorInfo cursorInfo
    ) {
        if (cursorInfo.id() == null) {
            return null;
        }

        return followingGroupScore.gt(cursorInfo.followingGroupScore())
                .or(followingGroupScore.eq(cursorInfo.followingGroupScore())
                        .and(nicknameScore.lt(cursorInfo.nicknameScore())
                                .or(nicknameScore.eq(cursorInfo.nicknameScore())
                                        .and(nameScore.lt(cursorInfo.nameScore())
                                                .or(nameScore.eq(cursorInfo.nameScore())
                                                        .and(createdAt.lt(cursorInfo.createdAt())
                                                                .or(createdAt.eq(cursorInfo.createdAt())
                                                                        .and(id.lt(cursorInfo.id())))
                                                        )
                                                )
                                        )
                                )
                        )
                );
    }

    private String encodeSearchCursor(MemberSearchRow row) {
        int followingGroupScore = row.isFollowing() ? 1 : 0;
        return followingGroupScore + "/" + row.nicknameScore() + "/" + row.nameScore() + "/" + row.createdAt() + "/" + row.id();
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
