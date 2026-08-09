package com.example.plimap.domain.place.repository.query.impl;

import com.example.plimap.domain.place.dto.PopularPlaceCandidate;
import com.example.plimap.domain.place.repository.query.PopularPlaceQueryRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PopularPlaceQueryRepositoryImpl implements PopularPlaceQueryRepository {

    private static final double NEARBY_RADIUS_METERS = 500.0;
    private static final double DISTANCE_PREFILTER_TOLERANCE_METERS = 0.001;
    private static final int RESULT_LIMIT = 6;
    private static final String ACTIVE_PIN_COUNTS = """
            WITH active_pin_counts AS MATERIALIZED (
                SELECT p.place_id, COUNT(*) AS pin_count
                FROM pin p
                WHERE p.deleted_at IS NULL
                GROUP BY p.place_id
            )
            """;
    private static final String GLOBAL_QUERY = ACTIVE_PIN_COUNTS + """
            SELECT
                pl.id,
                pl.name,
                ST_Distance(
                    pl.location,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                ) AS distance_meters,
                pc.pin_count
            FROM place pl
            JOIN active_pin_counts pc
              ON pc.place_id = pl.id
            WHERE pl.deleted_at IS NULL
            ORDER BY pin_count DESC, distance_meters ASC, pl.id ASC
            LIMIT :resultLimit
            """;
    private static final String NEARBY_QUERY = """
            SELECT
                pl.id,
                pl.name,
                ST_Distance(
                    pl.location,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                ) AS distance_meters,
                pc.pin_count
            FROM place pl
            JOIN LATERAL (
                SELECT COUNT(*) AS pin_count
                FROM pin p
                WHERE p.place_id = pl.id
                  AND p.deleted_at IS NULL
            ) pc ON pc.pin_count > 0
            WHERE pl.deleted_at IS NULL
              AND ST_DWithin(
                    pl.location,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    :prefilterRadiusMeters
                  )
              AND ST_Distance(
                    pl.location,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                  ) <= :radiusMeters
            ORDER BY distance_meters ASC, pin_count DESC, pl.id ASC
            LIMIT :resultLimit
            """;

    private final EntityManager entityManager;

    @Override
    public List<PopularPlaceCandidate> findNearbyPopularPlaces(
            double latitude,
            double longitude
    ) {
        return query(NEARBY_QUERY, latitude, longitude, true);
    }

    @Override
    public List<PopularPlaceCandidate> findGlobalPopularPlaces(
            double latitude,
            double longitude
    ) {
        return query(GLOBAL_QUERY, latitude, longitude, false);
    }

    @SuppressWarnings("unchecked")
    private List<PopularPlaceCandidate> query(
            String sql,
            double latitude,
            double longitude,
            boolean nearby
    ) {
        var query = entityManager.createNativeQuery(sql)
                .setParameter("latitude", latitude)
                .setParameter("longitude", longitude)
                .setParameter("resultLimit", RESULT_LIMIT);
        if (nearby) {
            query.setParameter("radiusMeters", NEARBY_RADIUS_METERS)
                    .setParameter(
                            "prefilterRadiusMeters",
                            NEARBY_RADIUS_METERS + DISTANCE_PREFILTER_TOLERANCE_METERS
                    );
        }

        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new PopularPlaceCandidate(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).doubleValue(),
                        ((Number) row[3]).longValue()
                ))
                .toList();
    }
}
