package com.example.plimap.domain.place.service.query;

import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.entity.Place;
import java.util.Optional;

public interface PlaceQueryService {

    PlaceResponse.SearchResult searchPlaces(PlaceRequest.Search request);

    Place getActivePlace(Long placeId);

    Optional<Place> findActivePlaceByProviderAndProviderPlaceId(
            String placeProvider,
            String providerPlaceId
    );
}
