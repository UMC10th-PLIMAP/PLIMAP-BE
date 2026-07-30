package com.example.plimap.domain.pin.repository.query.impl;

import com.example.plimap.domain.member.entity.QMember;
import com.example.plimap.domain.member.entity.QMemberFollow;
import com.example.plimap.domain.pin.converter.PinConverter;
import com.example.plimap.domain.pin.dto.CursorInfo;
import com.example.plimap.domain.pin.dto.Pagination;
import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.entity.QPin;
import com.example.plimap.domain.pin.entity.QPinLike;
import com.example.plimap.domain.pin.enums.PinSortType;
import com.example.plimap.domain.pin.exception.PinErrorCode;
import com.example.plimap.domain.pin.exception.PinException;
import com.example.plimap.domain.pin.repository.query.PinQueryRepository;
import com.example.plimap.domain.place.entity.QPlace;
import com.example.plimap.domain.report.entity.QReport;
import com.example.plimap.domain.track.entity.QPlaceTrack;
import com.example.plimap.domain.track.entity.QTrack;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.example.plimap.domain.pin.entity.QPinTag.pinTag;
import static com.example.plimap.domain.pin.entity.QTag.tag;

@Repository
@RequiredArgsConstructor
public class PinQueryRepositoryImpl implements PinQueryRepository {
    private static final double DISTANCE_METERS = 20.0;
    private static final double DISTANCE_PREFILTER_TOLERANCE_METERS = 0.001;
    private static final String NEAREST_ACTIVE_PIN_WITHIN_20M_QUERY = """
            SELECT ST_Distance(
                              pl.location,
                              ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                          ) AS distance
                   FROM pin p
                   JOIN place pl
                     ON p.place_id = pl.id
                   WHERE p.deleted_at IS NULL
                     AND pl.deleted_at IS NULL
                     AND ST_DWithin(
                           pl.location,
                           ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                           :distanceMeters + :prefilterToleranceMeters
                         )
                   ORDER BY distance
                   LIMIT 1
            """;
    private static final String SEARCH_FIRST_PIN_CREATOR_NICKNAME_QUERY = """
            SELECT DISTINCT ON (pl.id)
                   pl.id,
                   CASE WHEN m.status = 'WITHDRAWN' THEN '플리맵사용자' ELSE m.nickname END,
                   p.id,
                   COALESCE(pc.pin_count, 0) AS pin_count
            FROM place pl
            LEFT JOIN pin p 
                    ON p.place_id = pl.id 
                    AND p.deleted_at IS NULL
            LEFT JOIN member m 
                    ON p.member_id = m.id
            LEFT JOIN (
                SELECT place_id,
                       COUNT(*) AS pin_count
                FROM pin
                WHERE deleted_at IS NULL
                        AND place_id IN (:placeIds) 
                GROUP BY place_id
            ) pc
                ON pc.place_id = pl.id
            WHERE pl.id IN (:placeIds) 
                    AND pl.deleted_at IS NULL
            ORDER BY pl.id, p.created_at ASC, p.id ASC;
            """;

    private final EntityManager entityManager;
    private final JPAQueryFactory queryFactory;

    QPin pin = QPin.pin;
    QPinLike pinLike = QPinLike.pinLike;
    QMember member = QMember.member;
    QPlaceTrack placeTrack = QPlaceTrack.placeTrack;
    QTrack track = QTrack.track;
    QPlace place = QPlace.place;
    QMemberFollow memberFollow = QMemberFollow.memberFollow;
    QReport report = QReport.report;

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Double> findNearestActivePinWithin20m(
            double latitude, double longitude
    ) {
        return entityManager.createNativeQuery(NEAREST_ACTIVE_PIN_WITHIN_20M_QUERY)
                .setParameter("latitude", latitude)
                .setParameter("longitude", longitude)
                .setParameter("distanceMeters", DISTANCE_METERS)
                .setParameter("prefilterToleranceMeters", DISTANCE_PREFILTER_TOLERANCE_METERS)
                .getResultStream()
                .map(result -> ((Number) result).doubleValue())
                .findFirst();
    }

    @Override
    public Map<Long, PlacePinInfo> findPinInfosByPlaceIds(List<Long> placeIds) {
        List<Object[]> rows = entityManager
                .createNativeQuery(SEARCH_FIRST_PIN_CREATOR_NICKNAME_QUERY)
                .setParameter("placeIds", placeIds)
                .getResultList();

        Map<Long, PlacePinInfo> result = new HashMap<>();

        for (Object[] row : rows) {
            Long placeId = ((Number) row[0]).longValue();
            String nickname = (String) row[1];
            Number pinId = (Number) row[2];
            Long pinCount = (Long) row[3];

            boolean hasPin = pinId != null;
            result.put(placeId, new PlacePinInfo(hasPin, nickname, pinCount));
        }

        return result;
    }

    @Override
    public Pagination<PinResponse.Feed> findFeedListByMemberId(Long memberId, String cursor, Integer pageSize) {
        CursorInfo cursorInfo = parseCursor(cursor, null);

        List<Long> pinIds = queryFactory
                .select(pin.id)
                .from(pin)
                .where(
                        pin.member.id.eq(memberId),
                        cursorCondition(cursorInfo, null),
                        pin.deletedAt.isNull(),
                        pin.isFeedPublic.eq(true)
                )
                .orderBy(pin.createdAt.desc(), pin.id.desc())
                .limit(pageSize + 1)
                .fetch();

        if (pinIds.isEmpty()) {
            return emptyPagination(pageSize);
        }

        List<Pin> pins = queryFactory
                .selectDistinct(pin)
                .from(pin)
                .join(pin.placeTrack, placeTrack)
                .join(placeTrack.track, track)
                .join(placeTrack.place, place)
                .where(
                        pin.member.id.eq(memberId),
                        pin.id.in(pinIds),
                        cursorCondition(cursorInfo, null),
                        pin.isFeedPublic.eq(true),
                        pin.deletedAt.isNull()
                )
                .orderBy(pin.createdAt.desc(), pin.id.desc())
                .limit(pageSize + 1)
                .fetch();

        List<PinResponse.Feed> data = new ArrayList<>(pins.stream()
                .map(PinConverter::toFeed).toList());


        boolean hasNext = data.size() > pageSize;

        if (hasNext) {
            data.remove(pageSize.intValue());
        }

        if (data.isEmpty()) {
            return PinConverter.toPagination(
                    data,
                    null,
                    false,
                    pageSize
            );
        }


        PinResponse.Feed last = data.getLast();
        String nextCursor = hasNext
                ? last.createdAt() + "/" + last.pinId()
                : null;

        return PinConverter.toPagination(data, nextCursor, hasNext, pageSize);
    }

    @Override
    public Pagination<PinResponse.MyPin> findMyPinList(Long memberId, String cursor, Integer pageSize) {
        CursorInfo cursorInfo = parseCursor(cursor, null);

        List<Long> pinIds = queryFactory
                .select(pin.id)
                .from(pin)
                .where(
                        pin.member.id.eq(memberId),
                        cursorCondition(cursorInfo, null),
                        pin.deletedAt.isNull()
                )
                .orderBy(pin.createdAt.desc(), pin.id.desc())
                .limit(pageSize + 1)
                .fetch();

        if (pinIds.isEmpty()) {
            return emptyPagination(pageSize);
        }

        boolean hasNext = pinIds.size() > pageSize;

        if (hasNext) {
            pinIds.remove(pageSize.intValue());
        }

        List<Pin> pins = queryFactory
                .selectDistinct(pin)
                .from(pin)
                .join(pin.placeTrack, placeTrack).fetchJoin()
                .join(placeTrack.track, track).fetchJoin()
                .join(placeTrack.place, place).fetchJoin()
                .leftJoin(pin.pinTagList, pinTag).fetchJoin()
                .leftJoin(pinTag.tag, tag).fetchJoin()
                .where(
                        pin.id.in(pinIds),
                        place.deletedAt.isNull(),
                        placeTrack.deletedAt.isNull()
                )
                .orderBy(pin.createdAt.desc(), pin.id.desc())
                .fetch();

        List<PinResponse.MyPin> data = pins.stream()
                .map(PinConverter::toMyPin)
                .toList();

        if (data.isEmpty()) {
            return PinConverter.toPagination(
                    data,
                    null,
                    false,
                    pageSize
            );
        }

        PinResponse.MyPin last = data.getLast();
        String nextCursor = hasNext
                ? last.createdAt() + "/" + last.pinId()
                : null;

        return PinConverter.toPagination(data, nextCursor, hasNext, pageSize);
    }

    @Override
    public Boolean existsPinByMemberFollowAndPlace(Long memberId, Long placeId) {

        return queryFactory
                .selectOne()
                .from(memberFollow)
                .join(memberFollow.following, member)
                .join(pin).on(pin.member.eq(member))
                .join(place).on(pin.place.eq(place))
                .where(
                        memberFollow.follower.id.eq(memberId),
                        pin.place.id.eq(placeId),
                        pin.deletedAt.isNull(),
                        place.deletedAt.isNull(),
                        member.deletedAt.isNull()
                )
                .fetchFirst() != null;
    } 

    @Override
    public Pagination<PinResponse.PinDetail> findPinListByPlaceTrackIdAndSortType(Long memberId, String cursor, Integer pageSize, PinSortType pinSortType, Long placeTrackId) {
        pinSortType = pinSortType == null? PinSortType.LATEST : pinSortType;
        CursorInfo cursorInfo = parseCursor(cursor, pinSortType);

        List<Long> pinIds = queryFactory
                .select(pin.id)
                .from(pin)
                .leftJoin(report)
                .on(
                        report.reportedPin.eq(pin),
                        report.reporter.id.eq(memberId)
                )
                .where(
                        cursorCondition(cursorInfo, pinSortType),
                        pin.deletedAt.isNull(),
                        pin.placeTrack.id.eq(placeTrackId),
                        report.id.isNull()
                )
                .orderBy(getOrders(pinSortType))
                .limit(pageSize + 1)
                .fetch();

        if (pinIds.isEmpty()) {
            return emptyPagination(pageSize);
        }

        boolean hasNext = pinIds.size() > pageSize;

        if (hasNext) {
            pinIds.remove(pageSize.intValue());
        }

        List<Pin> pins = queryFactory
                .selectDistinct(pin)
                .from(pin)
                .join(pin.placeTrack, placeTrack).fetchJoin()
                .join(pin.member, member).fetchJoin()
                .leftJoin(pin.pinTagList, pinTag).fetchJoin()
                .leftJoin(pinTag.tag, tag).fetchJoin()
                .where(
                        pin.id.in(pinIds),
                        placeTrack.deletedAt.isNull(),
                        pin.deletedAt.isNull()
                )
                .orderBy(getOrders(pinSortType))
                .fetch();

        List<Long> likedPinIds = queryFactory
                .select(pinLike.pin.id)
                .from(pinLike)
                .where(
                        pinLike.member.id.eq(memberId),
                        pinLike.pin.id.in(pinIds)
                )
                .fetch();

        Set<Long> likedPinIdSet = new HashSet<>(likedPinIds);

        List<PinResponse.PinDetail> data = pins.stream()
                .map(p ->
                        PinConverter.toPinDetail(
                                p,
                                likedPinIdSet.contains(p.getId())
                        )
                )
                .toList();

        if (data.isEmpty()) {
            return PinConverter.toPagination(
                    data,
                    null,
                    false,
                    pageSize
            );
        }

        PinResponse.PinDetail last = data.getLast();
        String nextCursor = hasNext
                ? (pinSortType.equals(PinSortType.POPULAR)
                    ? last.likeCount() + "/" + last.pinId()
                    : last.createdAt() + "/" + last.pinId()
                )
                : null;

        return PinConverter.toPagination(data, nextCursor, hasNext, pageSize);
    }

    @Override
    public Optional<Pin> getPinPreview(Long pinId) {
        return Optional.ofNullable(queryFactory
                .select(pin)
                .from(pin)
                .join(pin.member, member).fetchJoin()
                .join(pin.place, place).fetchJoin()
                .join(pin.placeTrack, placeTrack).fetchJoin()
                .join(placeTrack.track, track).fetchJoin()
                .where(
                        pin.id.eq(pinId),
                        pin.deletedAt.isNull(),
                        placeTrack.deletedAt.isNull(),
                        place.deletedAt.isNull()
                )
                .fetchOne()
        );
    }

    private CursorInfo parseCursor(String cursor, PinSortType pinSortType) {
        if (cursor == null) {
            return new CursorInfo(null, null, null);
        }
        try {
            String[] parts = cursor.split("/");

            if (parts.length != 2) {
                throw new PinException(PinErrorCode.INVALID_CURSOR);
            }

            Instant createdAt = null;
            Integer like = null;
            if (pinSortType == PinSortType.POPULAR) {
                like = Integer.parseInt(parts[0]);
            }
            else {
                createdAt = Instant.parse(parts[0]);
            }
            long pinId = Long.parseLong(parts[1]);


            return new CursorInfo(createdAt, pinId, like);

        } catch (DateTimeParseException | NumberFormatException e) {
            throw new PinException(PinErrorCode.INVALID_CURSOR);
        }
    }

    private BooleanExpression cursorCondition(
            CursorInfo cursorInfo,
            PinSortType pinSortType
    ) {
        pinSortType = pinSortType == null? PinSortType.LATEST : pinSortType;
        return switch (pinSortType) {
            case LATEST -> {
                if (cursorInfo.createdAt() == null || cursorInfo.pinId() == null) {
                    yield null;
                }

                yield pin.createdAt.lt(cursorInfo.createdAt())
                        .or(
                                pin.createdAt.eq(cursorInfo.createdAt())
                                        .and(pin.id.lt(cursorInfo.pinId()))
                        );
            }

            case POPULAR -> {
                if (cursorInfo.likeCount() == null || cursorInfo.pinId() == null) {
                    yield null;
                }

                yield pin.likeCount.lt(cursorInfo.likeCount())
                        .or(
                                pin.likeCount.eq(cursorInfo.likeCount())
                                        .and(pin.id.lt(cursorInfo.pinId()))
                        );
            }
        };
    }

    private OrderSpecifier<?>[] getOrders(PinSortType pinSortType) {
        return switch (pinSortType) {
            case LATEST -> new OrderSpecifier[]{
                    pin.createdAt.desc(),
                    pin.id.desc()
            };
            case POPULAR -> new OrderSpecifier[]{
                    pin.likeCount.desc(),
                    pin.id.desc()
            };
        };
    }

    public static <T> Pagination<T> emptyPagination(Integer pageSize) {
        return new Pagination<>(
                List.of(),
                null,
                false,
                pageSize
        );
    }
}
