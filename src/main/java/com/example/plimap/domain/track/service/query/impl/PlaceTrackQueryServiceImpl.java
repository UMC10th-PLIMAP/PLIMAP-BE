package com.example.plimap.domain.track.service.query.impl;

import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.pin.validator.PinLocationValidator;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
import com.example.plimap.domain.track.converter.PlaceTrackConverter;
import com.example.plimap.domain.track.dto.PlaceTrackQueryResult;
import com.example.plimap.domain.track.dto.request.PlaceTrackRequest;
import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;
import com.example.plimap.domain.track.repository.query.PlaceTrackQueryRepository;
import com.example.plimap.domain.track.service.query.PlaceTrackQueryService;
import java.util.List;
import java.util.Map;
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
    private final PinQueryService pinQueryService;
    private final PinLocationValidator pinLocationValidator;
    private final PlaceTrackQueryRepository placeTrackQueryRepository;

    @Override
    public PlaceTrackResponse.ListResult getPlaceTracks(
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
        boolean bookmarked =
                placeTrackQueryRepository.existsPlaceBookmark(placeId, memberId);
        String createdBy = findFirstPinCreatorNickname(placeId);

        return PlaceTrackConverter.toListResult(
                place,
                createdBy,
                distance,
                withinRadius,
                bookmarked,
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

    private String findFirstPinCreatorNickname(Long placeId) {
        Map<Long, PlacePinInfo> pinInfos =
                pinQueryService.findPinInfosByPlaceIds(List.of(placeId));
        PlacePinInfo pinInfo = pinInfos.get(placeId);
        return pinInfo == null ? null : pinInfo.firstPinCreatorNickname();
    }
}
