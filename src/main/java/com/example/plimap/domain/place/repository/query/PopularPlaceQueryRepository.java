package com.example.plimap.domain.place.repository.query;

import com.example.plimap.domain.place.dto.PopularPlaceCandidate;
import java.util.List;

public interface PopularPlaceQueryRepository {

    List<PopularPlaceCandidate> findNearbyPopularPlaces(
            double latitude,
            double longitude
    );

    List<PopularPlaceCandidate> findGlobalPopularPlaces(
            double latitude,
            double longitude
    );
}
