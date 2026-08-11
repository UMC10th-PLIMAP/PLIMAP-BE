package com.example.plimap.domain.place.repository.query;

import com.example.plimap.domain.place.dto.PopularPlaceCandidate;
import java.util.List;

public interface PopularPlaceQueryRepository {

    List<PopularPlaceCandidate> findNearbyPopularPlaces(
            double latitude,
            double longitude
    );

    List<PopularPlaceCandidate> findRegion3PopularPlaces(
            String administrativeRegionCode,
            double latitude,
            double longitude
    );

    List<PopularPlaceCandidate> findRegion2PopularPlaces(
            String sido,
            String sigungu,
            double latitude,
            double longitude
    );

    List<PopularPlaceCandidate> findRegion1PopularPlaces(
            String sido,
            double latitude,
            double longitude
    );

    List<PopularPlaceCandidate> findGlobalPopularPlaces(
            double latitude,
            double longitude
    );
}
