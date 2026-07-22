package com.example.plimap.domain.pin.repository.query.impl;

import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.repository.query.PinQueryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
                   m.nickname
            FROM place pl
            LEFT JOIN pin p 
                    ON p.place_id = pl.id 
                    AND p.deleted_at IS NULL
            LEFT JOIN member m 
                    ON p.member_id = m.id
            WHERE pl.id IN (:placeIds)
            ORDER BY pl.id, p.created_at ASC, p.id ASC;
            """;

    private final EntityManager entityManager;

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

            if (nickname != null) {
                result.put(placeId, new PlacePinInfo(true, nickname));
                continue;
            }
            result.put(placeId, new PlacePinInfo(false, null));
        }

        return result;
    }
}
