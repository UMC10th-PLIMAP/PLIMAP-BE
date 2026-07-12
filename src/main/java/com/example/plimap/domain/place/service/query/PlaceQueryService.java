package com.example.plimap.domain.place.service.query;

import com.example.plimap.domain.place.entity.Place;
import java.util.Optional;

public interface PlaceQueryService {

    Place getActivePlace(Long placeId);

    Optional<Place> findActivePlaceByProviderAndProviderPlaceId(
            String placeProvider,
            String providerPlaceId
    );
}
