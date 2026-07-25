package com.example.plimap.domain.track.service.query.impl;

import com.example.plimap.domain.pin.validator.PinLocationValidator;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
import com.example.plimap.domain.track.converter.PlaceTrackConverter;
import com.example.plimap.domain.track.dto.PlaceTrackQueryResult;
import com.example.plimap.domain.track.dto.request.PlaceTrackRequest;
import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;
import com.example.plimap.domain.track.repository.query.PlaceTrackQueryRepository;
import com.example.plimap.domain.track.service.query.PlaceTrackQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceTrackQueryServiceImpl implements PlaceTrackQueryService {

    private static final double ACCESSIBLE_RADIUS_METERS = 500.0;

    private final PlaceQueryService placeQueryService;
    private final PinLocationValidator pinLocationValidator;
    private final PlaceTrackQueryRepository placeTrackQueryRepository;

    @Override
    public PlaceTrackResponse.PlaceTrackListResult getPlaceTracks(
            Long memberId,
            Long placeId,
            PlaceTrackRequest.List request
    ) {
        Place place = placeQueryService.getActivePlace(placeId);
        double distance = calculateDistance(request, place);
        boolean withinRadius = distance <= ACCESSIBLE_RADIUS_METERS;
        Pageable pageable = PageRequest.of(request.page(), request.size());

        Slice<PlaceTrackQueryResult> placeTracks =
                placeTrackQueryRepository.findPlaceTracks(
                        placeId,
                        memberId,
                        request.sort(),
                        pageable
                );
        return PlaceTrackConverter.toListResult(
                place,
                distance,
                withinRadius,
                placeTracks
        );
    }

    private double calculateDistance(PlaceTrackRequest.List request, Place place) {
        return pinLocationValidator.calculateDistance(
                request.latitude(),
                request.longitude(),
                place.getLocation().getY(),
                place.getLocation().getX()
        );
    }

}
