package com.example.plimap.domain.place.service.query.impl;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceQueryServiceImpl implements PlaceQueryService {

    private final PlaceRepository placeRepository;

    @Override
    public Place getActivePlace(Long placeId) {
        return placeRepository.findByIdAndDeletedAtIsNull(placeId)
                .orElseThrow(() -> new PlaceException(PlaceErrorCode.PLACE_NOT_FOUND));
    }

    @Override
    public Optional<Place> findActivePlaceByProviderAndProviderPlaceId(
            String placeProvider,
            String providerPlaceId
    ) {
        return placeRepository.findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                placeProvider,
                providerPlaceId
        );
    }
}
