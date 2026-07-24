package com.example.plimap.domain.pin.repository.query.impl;

import com.example.plimap.domain.pin.converter.PinConverter;
import com.example.plimap.domain.pin.dto.Pagination;
import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.QPin;
import com.example.plimap.domain.pin.repository.query.PinQueryRepository;
import com.example.plimap.domain.track.entity.QPlaceTrack;
import com.example.plimap.domain.track.entity.QTrack;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                   p.id
            FROM place pl
            LEFT JOIN pin p 
                    ON p.place_id = pl.id 
                    AND p.deleted_at IS NULL
            LEFT JOIN member m 
                    ON p.member_id = m.id
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

            boolean hasPin = pinId != null;
            result.put(placeId, new PlacePinInfo(hasPin, nickname));
        }

        return result;
    }

    @Override
    public Pagination<PinResponse.Feed> findFeedListByMemberId(Long memberId, String cursor, Integer pageSize) {
        QPin pin = QPin.pin;
        QPlaceTrack placeTrack = QPlaceTrack.placeTrack;
        QTrack track = QTrack.track;

        Instant cursorCreatedAt = null;
        Long cursorId = null;

        if (cursor != null) {
            String[] cursorSplit = cursor.split("/");
            cursorCreatedAt = Instant.parse(cursorSplit[0]);
            cursorId = Long.parseLong(cursorSplit[1]);
        }

        List<PinResponse.Feed> data = queryFactory
                .select(
                    Projections.constructor(
                            PinResponse.Feed.class,
                            pin.id,
                            track.albumImageUrl,
                            pin.createdAt
                    )
                )
                .from(pin)
                .join(pin.placeTrack, placeTrack)
                .join(placeTrack.track, track)
                .where(
                        pin.member.id.eq(memberId),
                        cursorCondition(cursorCreatedAt, cursorId)
                )
                .orderBy(pin.createdAt.desc(), pin.id.desc())
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = data.size() > pageSize;
        if (hasNext) {
            data.remove(pageSize.intValue());
        }
        PinResponse.Feed last = data.get(data.size() - 1);
        String nextCursor = hasNext
                ? last.createdAt() + "/" + last.pinId()
                : null;

        return PinConverter.toPagination(data, nextCursor, hasNext, data.size());
    }

    private String[] splitCursor(String cursor) {
        return cursor.split("/");
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
