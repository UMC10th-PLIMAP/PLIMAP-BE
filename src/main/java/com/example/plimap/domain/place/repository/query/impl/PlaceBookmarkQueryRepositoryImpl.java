package com.example.plimap.domain.place.repository.query.impl;

import com.example.plimap.domain.place.dto.NearbyBookmarkedPlace;
import com.example.plimap.domain.place.repository.query.PlaceBookmarkQueryRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlaceBookmarkQueryRepositoryImpl implements PlaceBookmarkQueryRepository {

    private static final int MAX_BOOKMARKS = 9;
    private static final double MAX_DISTANCE_METERS = 500.0;
    private static final double DISTANCE_PREFILTER_TOLERANCE_METERS = 0.001;
    private static final String FIND_NEARBY_ACTIVE_BOOKMARKS_QUERY = """
            WITH origin AS (
                SELECT ST_SetSRID(
                    ST_MakePoint(:longitude, :latitude),
                    4326
                )::geography AS location
            )
            SELECT p.id,
                   p.name,
                   ROUND(distance.value)::integer AS distance_meters
            FROM place_bookmark pb
            JOIN place p ON p.id = pb.place_id
            CROSS JOIN origin
            CROSS JOIN LATERAL (
                SELECT ST_Distance(p.location, origin.location) AS value
            ) distance
            WHERE pb.member_id = :memberId
              AND p.deleted_at IS NULL
              AND ST_DWithin(
                    p.location,
                    origin.location,
                    :maxDistanceMeters + :prefilterToleranceMeters
                  )
              AND distance.value <= :maxDistanceMeters
            ORDER BY distance_meters ASC,
                     pb.created_at DESC,
                     p.id ASC
            LIMIT :maxBookmarks
            """;

    private final EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<NearbyBookmarkedPlace> findNearbyActiveBookmarks(
            Long memberId,
            double latitude,
            double longitude
    ) {
        List<Object[]> rows = entityManager
                .createNativeQuery(FIND_NEARBY_ACTIVE_BOOKMARKS_QUERY)
                .setParameter("memberId", memberId)
                .setParameter("latitude", latitude)
                .setParameter("longitude", longitude)
                .setParameter("maxDistanceMeters", MAX_DISTANCE_METERS)
                .setParameter(
                        "prefilterToleranceMeters",
                        DISTANCE_PREFILTER_TOLERANCE_METERS
                )
                .setParameter("maxBookmarks", MAX_BOOKMARKS)
                .getResultList();

        return rows.stream()
                .map(row -> new NearbyBookmarkedPlace(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).intValue()
                ))
                .toList();
    }
}
