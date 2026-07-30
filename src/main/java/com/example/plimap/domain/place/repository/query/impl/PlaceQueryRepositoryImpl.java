package com.example.plimap.domain.place.repository.query.impl;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.repository.query.PlaceQueryRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlaceQueryRepositoryImpl implements PlaceQueryRepository {

    private static final double DISTANCE_PREFILTER_TOLERANCE_METERS = 0.001;
    private static final String NEAREST_ACTIVE_MAP_SELECTION_QUERY = """
            SELECT p.*
            FROM place p
            WHERE p.deleted_at IS NULL
              AND p.source = 'MAP_SELECTION'
              AND p.place_provider IS NULL
              AND p.provider_place_id IS NULL
              AND ST_DWithin(
                    p.location,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    :distanceMeters + :prefilterToleranceMeters
                  )
              AND ST_Distance(
                    p.location,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                  ) <= :distanceMeters
            ORDER BY ST_Distance(
                         p.location,
                         ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                     ),
                     p.id
            LIMIT 1
            """;
    private static final String NEAREST_ACTIVE_PROVIDER_PLACE_SEARCH_QUERY = """
            SELECT p.*
            FROM place p
            WHERE p.deleted_at IS NULL
              AND p.source = 'PLACE_SEARCH'
              AND p.place_provider IS NOT NULL
              AND p.provider_place_id IS NOT NULL
              AND ST_DWithin(
                    p.location,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    :distanceMeters + :prefilterToleranceMeters
                  )
              AND ST_Distance(
                    p.location,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                  ) <= :distanceMeters
            ORDER BY ST_Distance(
                         p.location,
                         ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                     ),
                     p.id
            LIMIT 1
            """;

    private final EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Place> findNearestActiveMapSelectionWithin(
            double latitude,
            double longitude,
            double distanceMeters
    ) {
        return entityManager.createNativeQuery(NEAREST_ACTIVE_MAP_SELECTION_QUERY, Place.class)
                .setParameter("latitude", latitude)
                .setParameter("longitude", longitude)
                .setParameter("distanceMeters", distanceMeters)
                .setParameter("prefilterToleranceMeters", DISTANCE_PREFILTER_TOLERANCE_METERS)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Place> findNearestActiveProviderPlaceSearchWithin(
            double latitude,
            double longitude,
            double distanceMeters
    ) {
        return entityManager
                .createNativeQuery(NEAREST_ACTIVE_PROVIDER_PLACE_SEARCH_QUERY, Place.class)
                .setParameter("latitude", latitude)
                .setParameter("longitude", longitude)
                .setParameter("distanceMeters", distanceMeters)
                .setParameter("prefilterToleranceMeters", DISTANCE_PREFILTER_TOLERANCE_METERS)
                .getResultList()
                .stream()
                .findFirst();
    }
}
