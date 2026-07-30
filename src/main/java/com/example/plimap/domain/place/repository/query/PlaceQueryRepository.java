package com.example.plimap.domain.place.repository.query;

import com.example.plimap.domain.place.entity.Place;
import java.util.Optional;

public interface PlaceQueryRepository {

    Optional<Place> findNearestActiveMapSelectionWithin(
            double latitude,
            double longitude,
            double distanceMeters
    );

    Optional<Place> findNearestActiveProviderPlaceSearchWithin(
            double latitude,
            double longitude,
            double distanceMeters
    );
}
