package com.example.plimap.domain.pin.repository.query.impl;

import com.example.plimap.domain.member.entity.QMember;
import com.example.plimap.domain.member.entity.QMemberFollow;
import com.example.plimap.domain.pin.converter.PinConverter;
import com.example.plimap.domain.pin.dto.RegionInfo;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.entity.QPin;
import com.example.plimap.domain.pin.entity.QPinLike;
import com.example.plimap.domain.pin.enums.ClusterLevel;
import com.example.plimap.domain.pin.repository.query.ClusterAndPinRepository;
import com.example.plimap.domain.place.entity.QPlace;
import com.example.plimap.domain.place.entity.QPlaceBookmark;
import com.example.plimap.domain.report.entity.QReport;
import com.example.plimap.domain.track.entity.QPlaceTrack;
import com.example.plimap.domain.track.entity.QTrack;
import com.example.plimap.global.external.storage.ProfileImageStorage;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ClusterAndPinRepositoryImpl implements ClusterAndPinRepository {

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

    private static final String BOOKMARKED_QUERY = """
        EXISTS (
            SELECT 1
            FROM place_bookmark pb
            WHERE pb.place_id = pl.id
              AND pb.member_id = :memberId
        )
        """;

    private static final String BOOKMARKED_ANONYMOUS_QUERY =
            "FALSE";

    private static final String CLUSTER_QUERY = """
            SELECT
                t.cluster_level,
                t.region_name,
                AVG(ST_Y(t.location::geometry)) AS latitude,
                AVG(ST_X(t.location::geometry)) AS longitude,
                COUNT(*) AS place_count,
                MIN(ST_Y(t.location::geometry)) AS sw_lat,
                MIN(ST_X(t.location::geometry)) AS sw_lng,
                MAX(ST_Y(t.location::geometry)) AS ne_lat,
                MAX(ST_X(t.location::geometry)) AS ne_lng,
                BOOL_OR(t.bookmarked) AS has_bookmarked_place
            FROM (
                SELECT
                    pl.id,
                    pl.location,
                    %s AS cluster_level,
                    %s AS region_name,
                    COUNT(p.id) AS pin_count,
                     %s AS bookmarked
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

    private static final String GEOHASH_CLUSTER_QUERY = """
            WITH place_geohash AS (
                SELECT
                    pl.id,
                    pl.location,
                    ST_GeoHash(pl.location::geometry, :precision) AS geohash,
                    %s AS bookmarked
                FROM place pl
                WHERE ST_Covers(
                     ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326),
                     pl.location::geometry
                 )
                AND pl.deleted_at IS NULL
                AND EXISTS (
                          SELECT 1
                          FROM pin p
                          WHERE p.place_id = pl.id
                            AND p.deleted_at IS NULL
                      )
            ),
            geohash_group AS (
                SELECT
                    geohash,
                    COUNT(DISTINCT id) AS place_count,
                    MIN(id) AS place_id,
                    AVG(ST_Y(location::geometry)) AS latitude,
                    AVG(ST_X(location::geometry)) AS longitude,
                    MIN(ST_Y(location::geometry)) AS sw_lat,
                    MIN(ST_X(location::geometry)) AS sw_lng,
                    MAX(ST_Y(location::geometry)) AS ne_lat,
                    MAX(ST_X(location::geometry)) AS ne_lng,
                    BOOL_OR(bookmarked) AS has_bookmarked_place
                FROM place_geohash
                GROUP BY geohash
            )
            SELECT *
            FROM geohash_group;
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

    private final EntityManager entityManager;
    private final JPAQueryFactory queryFactory;
    private final ProfileImageStorage profileImageStorage;

    QPin pin = QPin.pin;
    QMember member = QMember.member;
    QPlaceTrack placeTrack = QPlaceTrack.placeTrack;
    QTrack track = QTrack.track;
    QPlace place = QPlace.place;
    QPlaceBookmark placeBookmark = QPlaceBookmark.placeBookmark;

    @Override
    public List<PinResponse.PinPreview> findPinPreviewListByPlaceIds(List<Long> placeIds, Long memberId) {
        Map<Long, Boolean> bookmarkedMap = new HashMap<>();

        if (memberId != null) {
            List<Long> bookmarkedPlaceIds = queryFactory
                    .select(placeBookmark.place.id)
                    .from(placeBookmark)
                    .where(
                            placeBookmark.place.id.in(placeIds),
                            placeBookmark.id.memberId.eq(memberId)
                    )
                    .fetch();

            bookmarkedPlaceIds.forEach(placeId ->
                    bookmarkedMap.put(placeId, true)
            );
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
                .map(pin ->
                        PinConverter.toPinPreview(
                                pin,
                                profileImageStorage.getPublicUrlOrNull(
                                        pin.getMember().getProfileImageObjectKey()
                                ),
                                bookmarkedMap.getOrDefault(pin.getPlace().getId(), false)
                        )
                )
                .toList();
    }

    @Override
    public List<PinResponse.PinPreview> findPinPreviewListByViewport(Point minPoint, Point maxPoint, Long memberId) {
        @SuppressWarnings("unchecked")
        String bookmarkedQuery = memberId == null
                ? BOOKMARKED_ANONYMOUS_QUERY
                : BOOKMARKED_QUERY;

        String sql = PLACE_ID_IN_RANGE_QUERY.formatted(bookmarkedQuery);

        List<Long> placeIds = entityManager.createNativeQuery(sql)
                .setParameter("minLng", minPoint.getX())
                .setParameter("minLat", minPoint.getY())
                .setParameter("maxLng", maxPoint.getX())
                .setParameter("maxLat", maxPoint.getY())
                .getResultList();

        if (placeIds.isEmpty()) {
            return List.of();
        }

        return findPinPreviewListByPlaceIds(placeIds, memberId);
    }

    @Override
    public List<PinResponse.Cluster> findClusterListByViewport(
            Point minPoint,
            Point maxPoint,
            Integer zoomLevel,
            Long memberId
    ) {
        RegionInfo info = getRegionInfo(zoomLevel);

        String bookmarkedQuery = memberId == null
                ? BOOKMARKED_ANONYMOUS_QUERY
                : BOOKMARKED_QUERY;

        String sql = CLUSTER_QUERY.formatted(
                info.clusterLevelSql(),
                info.regionNameSql(),
                bookmarkedQuery,
                info.regionNameSql()
        );

        Query query = entityManager.createNativeQuery(sql)
                .setParameter("minLng", minPoint.getX())
                .setParameter("minLat", minPoint.getY())
                .setParameter("maxLng", maxPoint.getX())
                .setParameter("maxLat", maxPoint.getY());

        if (memberId != null) {
            query.setParameter("memberId", memberId);
        }

        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(r -> {
                    return toCluster(r, null);
                })
                .toList();
    }

    @Override
    public PinResponse.ClusterAndPin findGeohashClusterListByViewport(Point minPoint, Point maxPoint, Integer zoomLevel, Integer precision, Long memberId) {
        String bookmarkedQuery = memberId == null
                ? BOOKMARKED_ANONYMOUS_QUERY
                : BOOKMARKED_QUERY;

        String sql = GEOHASH_CLUSTER_QUERY.formatted(bookmarkedQuery);

        Query query = entityManager.createNativeQuery(sql)
                .setParameter("precision", precision)
                .setParameter("minLng", minPoint.getX())
                .setParameter("minLat", minPoint.getY())
                .setParameter("maxLng", maxPoint.getX())
                .setParameter("maxLat", maxPoint.getY());

        if (memberId != null) {
            query.setParameter("memberId", memberId);
        }

        List<Object[]> rows = query.getResultList();


        List<PinResponse.Cluster> clusters = new ArrayList<>();
        List<Long> singlePlaceIds = new ArrayList<>();
        for (Object[] row : rows) {
            int placeCount = ((Number) row[1]).intValue();
            Long placeId = ((Number) row[2]).longValue();

            if (placeCount == 1) {
                singlePlaceIds.add(placeId);
            } else {
                clusters.add(
                        toCluster(row, precision)
                );
            }
        }

        List<PinResponse.PinPreview> pinPreviews = findPinPreviewListByPlaceIds(singlePlaceIds, memberId);

        return PinConverter.toClusterAndPin(clusters, pinPreviews, zoomLevel);
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

    private PinResponse.Cluster   toCluster(Object[] row, Integer precision) {
        if (precision != null) {
            return new PinResponse.Cluster(
                    ClusterLevel.GEOHASH,
                    null,
                    precision,
                    ((Number) row[3]).doubleValue(),
                    ((Number) row[4]).doubleValue(),
                    ((Number) row[1]).intValue(),
                    new PinResponse.Bound(
                            ((Number) row[5]).doubleValue(),
                            ((Number) row[6]).doubleValue(),
                            ((Number) row[7]).doubleValue(),
                            ((Number) row[8]).doubleValue()
                    ),
                    ((boolean) row[9])

            );
        }

        return new PinResponse.Cluster(
                ClusterLevel.valueOf((String) row[0]),
                (String) row[1],
                null,
                ((Number) row[2]).doubleValue(),
                ((Number) row[3]).doubleValue(),
                ((Number) row[4]).intValue(),
                new PinResponse.Bound(
                        ((Number) row[5]).doubleValue(),
                        ((Number) row[6]).doubleValue(),
                        ((Number) row[7]).doubleValue(),
                        ((Number) row[8]).doubleValue()
                ),
                ((boolean) row[9])

        );
    }
}
