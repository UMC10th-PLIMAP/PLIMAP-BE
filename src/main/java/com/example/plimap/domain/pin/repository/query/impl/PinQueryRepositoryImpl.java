package com.example.plimap.domain.pin.repository.query.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.entity.QMember;
import com.example.plimap.domain.member.entity.QMemberFollow;
import com.example.plimap.domain.pin.converter.PinConverter;
import com.example.plimap.domain.pin.dto.CursorInfo;
import com.example.plimap.domain.pin.dto.Pagination;
import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.entity.QPin;
import com.example.plimap.domain.pin.exception.PinErrorCode;
import com.example.plimap.domain.pin.exception.PinException;
import com.example.plimap.domain.pin.repository.query.PinQueryRepository;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.QPlace;
import com.example.plimap.domain.track.entity.QPlaceTrack;
import com.example.plimap.domain.track.entity.QTrack;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
                   m.nickname,
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
        QPin pin = QPin.pin;
        QPlaceTrack placeTrack = QPlaceTrack.placeTrack;
        QPlace place = QPlace.place;
        QTrack track = QTrack.track;
        CursorInfo cursorInfo = parseCursor(cursor);

        List<Long> pinIds = queryFactory
                .select(pin.id)
                .from(pin)
                .where(
                        pin.member.id.eq(memberId),
                        cursorCondition(cursorInfo.createdAt(), cursorInfo.pinId()),
                        pin.deletedAt.isNull(),
                        pin.isFeedPublic.eq(true)
                )
                .orderBy(pin.createdAt.desc(), pin.id.desc())
                .limit(pageSize + 1)
                .fetch();

        List<Pin> pins = queryFactory
                .selectDistinct(pin)
                .from(pin)
                .join(pin.placeTrack, placeTrack)
                .join(placeTrack.track, track)
                .join(placeTrack.place, place)
                .where(
                        pin.member.id.eq(memberId),
                        pin.id.in(pinIds),
                        cursorCondition(cursorInfo.createdAt(), cursorInfo.pinId()),
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
        QPin pin = QPin.pin;
        QPlaceTrack placeTrack = QPlaceTrack.placeTrack;
        QTrack track = QTrack.track;
        QPlace place = QPlace.place;
        CursorInfo cursorInfo = parseCursor(cursor);

        List<Long> pinIds = queryFactory
                .select(pin.id)
                .from(pin)
                .where(
                        pin.member.id.eq(memberId),
                        cursorCondition(cursorInfo.createdAt(), cursorInfo.pinId()),
                        pin.deletedAt.isNull()
                )
                .orderBy(pin.createdAt.desc(), pin.id.desc())
                .limit(pageSize + 1)
                .fetch();

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
        QPin pin = QPin.pin;
        QMember member = QMember.member;
        QPlace place = QPlace.place;
        QMemberFollow memberFollow = QMemberFollow.memberFollow;

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

    private CursorInfo parseCursor(String cursor) {
        if (cursor == null) {
            return new CursorInfo(null, null);
        }
        try {
            String[] parts = cursor.split("/");

            if (parts.length != 2) {
                throw new PinException(PinErrorCode.INVALID_CURSOR);
            }

            Instant createdAt = Instant.parse(parts[0]);
            long pinId = Long.parseLong(parts[1]);

            return new CursorInfo(createdAt, pinId);

        } catch (DateTimeParseException | NumberFormatException e) {
            throw new PinException(PinErrorCode.INVALID_CURSOR);
        }
    }

    private BooleanExpression cursorCondition(
            Instant createdAt,
            Long id
    ) {
        QPin pin = QPin.pin;

        if (createdAt == null || id == null) {
            return null;
        }

        return pin.createdAt.lt(createdAt)
                .or(
                        pin.createdAt.eq(createdAt)
                                .and(pin.id.lt(id))
                );
    }
}
