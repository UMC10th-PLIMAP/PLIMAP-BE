package com.example.plimap.domain.pin.repository.query.impl;

import com.example.plimap.domain.member.entity.QMember;
import com.example.plimap.domain.member.entity.QMemberFollow;
import com.example.plimap.domain.pin.converter.PinConverter;
import com.example.plimap.domain.pin.dto.*;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.entity.QPin;
import com.example.plimap.domain.pin.entity.QPinLike;
import com.example.plimap.domain.pin.enums.ClusterLevel;
import com.example.plimap.domain.pin.enums.PinSortType;
import com.example.plimap.domain.pin.exception.PinErrorCode;
import com.example.plimap.domain.pin.exception.PinException;
import com.example.plimap.domain.pin.repository.query.PinQueryRepository;
import com.example.plimap.domain.place.entity.QPlace;
import com.example.plimap.domain.report.entity.QReport;
import com.example.plimap.domain.track.converter.PlaceTrackConverter;
import com.example.plimap.domain.track.dto.AlbumImage;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.QPlaceTrack;
import com.example.plimap.domain.track.entity.QTrack;
import com.example.plimap.domain.track.entity.Track;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.plimap.domain.pin.entity.QPinTag.pinTag;
import static com.example.plimap.domain.pin.entity.QTag.tag;

@Repository
@RequiredArgsConstructor
public class PinQueryRepositoryImpl implements PinQueryRepository {
    private static final double DISTANCE_METERS = 20.0;
    private static final double DISTANCE_PREFILTER_TOLERANCE_METERS = 0.001;
    private static final int REPORT_HIDE_THRESHOLD = 10;
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

    private static final String PLACE_ID_IN_RANGE_QUERY = """
            SELECT DISTINCT pl.id
             FROM pin p
             JOIN place pl ON pl.id = p.place_id
             WHERE
                 ST_Covers(
                     ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326),
                     pl.location::geometry
                 )
                 AND p.deleted_at IS NULL
                 AND pl.deleted_at IS NULL
            """;

    private static final String REPRESENTATIVE_PLACE_TRACK = """
                representative_place_track AS (
                    SELECT DISTINCT ON (pt.place_id)
                        pt.place_id,
                        pt.id AS place_track_id
                    FROM place_track pt
                    JOIN place pl
                        ON pl.id = pt.place_id
                    JOIN pin p
                            ON p.place_track_id = pt.id
                           AND p.deleted_at IS NULL
                    LEFT JOIN pin_count pc
                        ON pc.place_track_id = pt.id
                    WHERE
                        pt.place_id IN (:placeIds)
                        AND pl.deleted_at IS NULL
                        AND pt.deleted_at IS NULL
                    ORDER BY
                        pt.place_id,
                        pt.like_count DESC,
                        COALESCE(pc.active_pin_count, 0) DESC,
                        pt.created_at DESC,
                        pt.id DESC
                )
                """;

    private static final String REPRESENTATIVE_PIN = """
            SELECT DISTINCT ON (pt.place_id)
                            p.id
                        FROM representative_place_track rpt
                        JOIN pin p
                            ON p.place_track_id = rpt.place_track_id
                        JOIN place_track pt
                            ON pt.id = p.place_track_id
                        WHERE
                            p.deleted_at IS NULL
                        ORDER BY
                            pt.place_id,
                            p.like_count DESC,
                            p.created_at DESC,
                            p.id DESC;
            """;


    private static final String PIN_COUNT= """
             WITH pin_count AS (
                 SELECT
                     p.place_track_id,
                     COUNT(*) AS active_pin_count
                 FROM pin p
                 WHERE p.deleted_at IS NULL
                 GROUP BY p.place_track_id
             )
    """;


    private static final String CLUSTER_QUERY = """
        SELECT
            t.cluster_level,
            t.region_name,
            AVG(ST_Y(t.location::geometry)) AS latitude,
            AVG(ST_X(t.location::geometry)) AS longitude,
            SUM(t.pin_count) AS pin_count,
            MIN(ST_Y(t.location::geometry)) AS sw_lat,
            MIN(ST_X(t.location::geometry)) AS sw_lng,
            MAX(ST_Y(t.location::geometry)) AS ne_lat,
            MAX(ST_X(t.location::geometry)) AS ne_lng
        FROM (
            SELECT
                pl.id,
                pl.location,
                %s AS cluster_level,
                %s AS region_name,
                COUNT(p.id) AS pin_count
            FROM pin p
            JOIN place pl ON pl.id = p.place_id
            WHERE
                ST_Covers(
                     ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326),
                     pl.location::geometry
                 )
                AND p.deleted_at IS NULL
                AND pl.deleted_at IS NULL
            GROUP BY
                pl.id,
                pl.location,
                %s
        ) t
        GROUP BY
            t.cluster_level,
            t.region_name
    """;

    private static final String FEED_QUERY =  """
        SELECT
            p.id,
            t.album_image_url,
            ST_Y(pl.location::geometry) AS latitude,
            ST_X(pl.location::geometry) AS longitude,
            pl.name,
            CAST(
                ST_DistanceSphere(
                    pl.location::geometry,
                    ST_SetSRID(ST_Point(:userLng, :userLat), 4326)
                ) AS INTEGER
            ) AS distance_from_user,
            (
                SELECT COUNT(*)
                FROM pin p2
                JOIN place_track pt2 ON pt2.id = p2.place_track_id
                WHERE pt2.place_id = pl.id
                  AND p2.deleted_at IS NULL
                  AND pt2.deleted_at IS NULL
            ) AS pin_count,
            p.created_at
        FROM pin p
        JOIN place_track pt ON pt.id = p.place_track_id
        JOIN track t ON t.id = pt.track_id
        JOIN place pl ON pl.id = pt.place_id
        WHERE p.id IN (:pinIds)
          AND p.member_id = :memberId
          AND p.deleted_at IS NULL
          AND pl.deleted_at IS NULL
          AND pt.deleted_at IS NULL
          AND p.is_feed_public = true
          AND p.report_count < %d
        ORDER BY p.created_at DESC, p.id DESC
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
    public Pagination<PinResponse.Feed> findFeedListByMemberId(Long memberId, Long viewerId, String cursor, Integer pageSize, PinRequest.UserLocation request) {
        CursorInfo cursorInfo = parseCursor(cursor, null);

        List<Long> pinIds = queryFactory
                .select(pin.id)
                .from(pin)
                .where(
                        pin.member.id.eq(memberId),
                        cursorCondition(cursorInfo, null),
                        pin.deletedAt.isNull(),
                        pin.isFeedPublic.eq(true),
                        pin.reportCount.lt(REPORT_HIDE_THRESHOLD),
                        notReportedByViewer(viewerId)
                )
                .orderBy(pin.createdAt.desc(), pin.id.desc())
                .limit(pageSize + 1)
                .fetch();

        if (pinIds.isEmpty()) {
            return emptyPagination(pageSize);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(FEED_QUERY.formatted(REPORT_HIDE_THRESHOLD))
                .setParameter("userLng", request.userLongitude())
                .setParameter("userLat", request.userLatitude())
                .setParameter("memberId", memberId)
                .setParameter("pinIds", pinIds)
                .getResultList();

        List<PinResponse.Feed> data = new ArrayList<>(rows.stream()
                .map(row -> PinResponse.Feed.builder()
                        .pinId(((Number) row[0]).longValue())
                        .albumImageUrl((String) row[1])
                        .latitude(((Number) row[2]).doubleValue())
                        .longitude(((Number) row[3]).doubleValue())
                        .placeName((String) row[4])
                        .distanceFromUser(((Number) row[5]).intValue())
                        .pinCount(((Number) row[6]).longValue())
                        .createdAt((Instant) row[7])
                        .build())
                .toList());

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
                        pin.deletedAt.isNull(),
                        pin.reportCount.lt(REPORT_HIDE_THRESHOLD)
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
                        placeTrack.deletedAt.isNull(),
                        pin.reportCount.lt(REPORT_HIDE_THRESHOLD)
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
                        report.id.isNull(),
                        pin.reportCount.lt(REPORT_HIDE_THRESHOLD)
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
                .leftJoin(report)
                .on(
                        report.reportedPin.eq(pin),
                        report.reporter.id.eq(memberId)
                )
                .where(
                        pin.id.in(pinIds),
                        placeTrack.deletedAt.isNull(),
                        pin.deletedAt.isNull(),
                        report.id.isNull(),
                        pin.reportCount.lt(REPORT_HIDE_THRESHOLD)
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
                                likedPinIdSet.contains(p.getId()),
                                p.getMember().getId().equals(memberId)
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
    public Optional<Pin> getPinPreview(Long pinId, Long viewerId) {
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
                        place.deletedAt.isNull(),
                        pin.reportCount.lt(REPORT_HIDE_THRESHOLD),
                        notReportedByViewer(viewerId)
                )
                .fetchOne()
        );
    }

    private BooleanExpression notReportedByViewer(Long viewerId) {
        if (viewerId == null) {
            return null;
        }
        return JPAExpressions
                .selectOne()
                .from(report)
                .where(report.reportedPin.eq(pin), report.reporter.id.eq(viewerId))
                .notExists();
    }

    @Override
    public boolean existsActivePinByPlaceIdAndMemberId(Long placeId, Long memberId) {
            return queryFactory
                    .selectOne()
                    .from(pin)
                    .join(pin.member, member)
                    .join(pin.place, place)
                    .where(
                            place.id.eq(placeId),
                            member.id.eq(memberId),
                            pin.deletedAt.isNull(),
                            member.deletedAt.isNull(),
                            place.deletedAt.isNull()
                    )
                    .fetchFirst() != null;
    }

    @Override
    public List<PinResponse.PinPreview> findPinPreviewListByViewport(Point minPoint, Point maxPoint) {
        @SuppressWarnings("unchecked")
        List<Long> placeIds = entityManager.createNativeQuery(PLACE_ID_IN_RANGE_QUERY)
                .setParameter("minLng", minPoint.getX())
                .setParameter("minLat", minPoint.getY())
                .setParameter("maxLng", maxPoint.getX())
                .setParameter("maxLat", maxPoint.getY())
                .getResultList();

        if (placeIds.isEmpty()) {
            return List.of();
        }

        List<Long> pinIds = entityManager.createNativeQuery(
                        PIN_COUNT
                                + ","
                                + REPRESENTATIVE_PLACE_TRACK
                                + REPRESENTATIVE_PIN
                )
                .setParameter("placeIds", placeIds)
                .getResultList();

        List<Pin> pins = queryFactory
                .selectFrom(pin)
                .join(pin.place, place).fetchJoin()
                .join(pin.member, member).fetchJoin()
                .join(pin.placeTrack, placeTrack).fetchJoin()
                .join(placeTrack.track, track).fetchJoin()
                .where(pin.id.in(pinIds))
                .fetch();

        return pins.stream()
                .map(PinConverter::toPinPreview)
                .toList();
    }

    @Override
    public List<PinResponse.Cluster> findClusterListByViewport(
            Point minPoint,
            Point maxPoint,
            Integer zoomLevel
    ) {
        RegionInfo info = getRegionInfo(zoomLevel);

        String sql = CLUSTER_QUERY.formatted(
            info.clusterLevelSql(),
            info.regionNameSql(),
            info.regionNameSql()
        );

        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("minLng", minPoint.getX())
                .setParameter("minLat", minPoint.getY())
                .setParameter("maxLng", maxPoint.getX())
                .setParameter("maxLat", maxPoint.getY())
                .getResultList();

        return rows.stream()
                .map(this::toCluster)
                .toList();
    }

    @Override
    public Pagination<PinResponse.FriendPin> getFriendRecentPinList(Long memberId, String cursor, Integer pageSize) {
        CursorInfo cursorInfo = parseCursor(cursor, null);

        List<Long> pinIds = queryFactory
                .select(pin.id)
                .from(pin)
                .join(pin.member, member)
                .join(memberFollow)
                .on(
                    memberFollow.follower.id.eq(memberId)
                            .and(memberFollow.following.id.eq(pin.member.id))
                )
                .where(
                        cursorCondition(cursorInfo, null),
                        pin.deletedAt.isNull(),
                        pin.createdAt.goe(Instant.now().minus(24, ChronoUnit.HOURS)),
                        pin.isFeedPublic.isTrue(),
                        member.deletedAt.isNull()
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
                .join(memberFollow)
                .on(
                        memberFollow.follower.id.eq(memberId)
                                .and(memberFollow.following.id.eq(pin.member.id))
                )
                .join(pin.placeTrack, placeTrack).fetchJoin()
                .join(pin.place, place).fetchJoin()
                .join(placeTrack.track, track).fetchJoin()
                .join(pin.member, member).fetchJoin()
                .where(
                        pin.id.in(pinIds),
                        pin.deletedAt.isNull(),
                        member.deletedAt.isNull(),
                        placeTrack.deletedAt.isNull(),
                        pin.isFeedPublic.isTrue()
                )
                .orderBy(pin.createdAt.desc(), pin.id.desc())
                .fetch();

        List<PinResponse.FriendPin> data = pins.stream()
                .map(PinConverter::toFriendPin).toList();

        if (data.isEmpty()) {
            return PinConverter.toPagination(
                    data,
                    null,
                    false,
                    pageSize
            );
        }

        PinResponse.FriendPin last = data.getLast();
        String nextCursor = hasNext
                ? last.createdAt() + "/" + last.pinId()
                : null;

        return PinConverter.toPagination(data, nextCursor, hasNext, pageSize);
    }

    @Override
    public Map<Long, AlbumImage> findRepresentativePlaceTracksByPlaceIds(List<Long> placeIds) {
        List<Long> placeTrackIds = entityManager.createNativeQuery(
                        PIN_COUNT
                                + ","
                                + REPRESENTATIVE_PLACE_TRACK
                                + """
                                    SELECT rpt.place_track_id
                                    FROM representative_place_track rpt
                                """
                )
                .setParameter("placeIds", placeIds)
                .getResultList();

        List<PlaceTrack> placeTracks = queryFactory
                .select(placeTrack)
                .from(placeTrack)
                .join(placeTrack.track, track).fetchJoin()
                .join(placeTrack.place, place).fetchJoin()
                .where(placeTrack.id.in(placeTrackIds))
                .fetch();

        return placeTracks.stream()
                .collect(Collectors.toMap(
                        pt -> pt.getPlace().getId(),
                        PlaceTrackConverter::toAlbumImage
                ));
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

    private RegionInfo getRegionInfo(Integer zoomLevel) {
        if (zoomLevel <= 7) {
            return new RegionInfo(
                    """
                    CASE
                        WHEN pl.sido IS NOT NULL THEN pl.sido
                        WHEN pl.sigungu IS NOT NULL THEN CONCAT(pl.sido, ' ', pl.sigungu)
                        ELSE CONCAT(pl.sido, ' ', pl.eup_myeon_dong)
                    END
                    """,
                    """
                    CASE
                        WHEN pl.sido IS NOT NULL THEN 'REGION1'
                        WHEN pl.sigungu IS NOT NULL THEN 'REGION2'
                        ELSE 'REGION3'
                    END
                    """
            );
        }

        if (zoomLevel <= 10) {
            return new RegionInfo(
                    """
                    CASE
                        WHEN pl.sigungu IS NOT NULL THEN CONCAT(pl.sido, ' ', pl.sigungu)
                        ELSE CONCAT(pl.sido, ' ', pl.eup_myeon_dong)
                    END
                    """,
                    """
                    CASE
                        WHEN pl.sigungu IS NOT NULL THEN 'REGION2'
                        ELSE 'REGION3'
                    END
                    """
            );
        }

        return new RegionInfo(
                """
                CASE
                    WHEN pl.sigungu IS NOT NULL
                        THEN CONCAT(pl.sido, ' ', pl.sigungu, ' ', pl.eup_myeon_dong)
                    ELSE
                        CONCAT(pl.sido, ' ', pl.eup_myeon_dong)
                END
                """,
                "'REGION3'"
        );
    }

    private PinResponse.Cluster toCluster(Object[] row) {
        return new PinResponse.Cluster(
                ClusterLevel.valueOf((String) row[0]),
                (String) row[1],
                ((Number) row[2]).doubleValue(),
                ((Number) row[3]).doubleValue(),
                ((Number) row[4]).intValue(),
                new PinResponse.Bound(
                        ((Number) row[5]).doubleValue(),
                        ((Number) row[6]).doubleValue(),
                        ((Number) row[7]).doubleValue(),
                        ((Number) row[8]).doubleValue()
                )
        );
    }
}
