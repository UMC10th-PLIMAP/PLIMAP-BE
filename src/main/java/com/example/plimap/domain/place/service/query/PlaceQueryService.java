package com.example.plimap.domain.place.service.query;

import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.entity.Place;
import jakarta.validation.Valid;
import java.util.Optional;

public interface PlaceQueryService {

    PlaceResponse.SearchResult searchPlaces(@Valid PlaceRequest.Search request);

    Place getActivePlace(Long placeId);

    Optional<Place> findActivePlaceByProviderAndProviderPlaceId(
            String placeProvider,
            String providerPlaceId
    );
}
