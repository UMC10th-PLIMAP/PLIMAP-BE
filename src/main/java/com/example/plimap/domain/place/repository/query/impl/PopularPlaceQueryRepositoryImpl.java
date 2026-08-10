package com.example.plimap.domain.place.repository.query.impl;

import com.example.plimap.domain.place.dto.PopularPlaceCandidate;
import com.example.plimap.domain.place.repository.query.PopularPlaceQueryRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PopularPlaceQueryRepositoryImpl implements PopularPlaceQueryRepository {

    private static final int RESULT_LIMIT = 6;
    private static final String ACTIVE_PIN_COUNTS = """
            WITH active_pin_counts AS MATERIALIZED (
                SELECT p.place_id, COUNT(*) AS pin_count
                FROM pin p
                WHERE p.deleted_at IS NULL
                GROUP BY p.place_id
            )
            """;
    private static final String BASE_QUERY = ACTIVE_PIN_COUNTS + """
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
            """;
    private static final String REGIONAL_BASE_QUERY = """
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
            """;
    private static final String POPULAR_ORDER_AND_LIMIT = """
            ORDER BY pin_count DESC, distance_meters ASC, pl.id ASC
            LIMIT :resultLimit
            """;
    private static final String NEARBY_QUERY = ACTIVE_PIN_COUNTS + """
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
            ORDER BY distance_meters ASC, pin_count DESC, pl.id ASC
            LIMIT :resultLimit
            """;
    private static final String REGION3_QUERY = REGIONAL_BASE_QUERY + """
              AND pl.administrative_region_code = :administrativeRegionCode
            """ + POPULAR_ORDER_AND_LIMIT;
    private static final String REGION2_QUERY = REGIONAL_BASE_QUERY + """
              AND pl.sido = :sido
              AND pl.sigungu = :sigungu
            """ + POPULAR_ORDER_AND_LIMIT;
    private static final String REGION1_QUERY = REGIONAL_BASE_QUERY + """
              AND pl.sido = :sido
            """ + POPULAR_ORDER_AND_LIMIT;
    private static final String GLOBAL_QUERY = BASE_QUERY + POPULAR_ORDER_AND_LIMIT;

    private final EntityManager entityManager;

    @Override
    public List<PopularPlaceCandidate> findNearbyPopularPlaces(
            double latitude,
            double longitude
    ) {
        return query(NEARBY_QUERY, latitude, longitude, Map.of());
    }

    @Override
    public List<PopularPlaceCandidate> findRegion3PopularPlaces(
            String administrativeRegionCode,
            double latitude,
            double longitude
    ) {
        return query(
                REGION3_QUERY,
                latitude,
                longitude,
                Map.of("administrativeRegionCode", administrativeRegionCode)
        );
    }

    @Override
    public List<PopularPlaceCandidate> findRegion2PopularPlaces(
            String sido,
            String sigungu,
            double latitude,
            double longitude
    ) {
        return query(
                REGION2_QUERY,
                latitude,
                longitude,
                Map.of("sido", sido, "sigungu", sigungu)
        );
    }

    @Override
    public List<PopularPlaceCandidate> findRegion1PopularPlaces(
            String sido,
            double latitude,
            double longitude
    ) {
        return query(REGION1_QUERY, latitude, longitude, Map.of("sido", sido));
    }

    @Override
    public List<PopularPlaceCandidate> findGlobalPopularPlaces(
            double latitude,
            double longitude
    ) {
        return query(GLOBAL_QUERY, latitude, longitude, Map.of());
    }

    @SuppressWarnings("unchecked")
    private List<PopularPlaceCandidate> query(
            String sql,
            double latitude,
            double longitude,
            Map<String, Object> parameters
    ) {
        var query = entityManager.createNativeQuery(sql)
                .setParameter("latitude", latitude)
                .setParameter("longitude", longitude)
                .setParameter("resultLimit", RESULT_LIMIT);
        parameters.forEach(query::setParameter);

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
