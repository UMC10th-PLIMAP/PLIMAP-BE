package com.example.plimap.domain.track.service.query.impl;

import com.example.plimap.domain.pin.validator.PinLocationValidator;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
import com.example.plimap.domain.track.converter.PlaceTrackConverter;
import com.example.plimap.domain.track.dto.LikedPlaceTrackQueryResult;
import com.example.plimap.domain.track.dto.PlaceTrackQueryResult;
import com.example.plimap.domain.track.dto.request.PlaceTrackRequest;
import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.PlaceTrackLikeId;
import com.example.plimap.domain.track.exception.TrackErrorCode;
import com.example.plimap.domain.track.exception.TrackException;
import com.example.plimap.domain.track.repository.PlaceTrackLikeRepository;
import com.example.plimap.domain.track.repository.PlaceTrackRepository;
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
    private final PlaceTrackRepository placeTrackRepository;
    private final PlaceTrackLikeRepository placeTrackLikeRepository;
    private final PlaceTrackQueryRepository placeTrackQueryRepository;

    @Override
    public PlaceTrackResponse.LikedPlaceTrackListResult getLikedPlaceTracks(
            Long memberId,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Slice<LikedPlaceTrackQueryResult> placeTracks =
                placeTrackQueryRepository.findLikedPlaceTracks(
                        memberId,
                        pageable
                );

        return PlaceTrackConverter.toLikedListResult(placeTracks);
    }

    @Override
    public PlaceTrackResponse.PlaceTrackDetail getPlaceTrackDetail(
            Long memberId,
            Long placeTrackId
    ) {
        PlaceTrack placeTrack = placeTrackRepository
                .findDetailByIdAndDeletedAtIsNull(placeTrackId)
                .orElseThrow(() -> new TrackException(
                        TrackErrorCode.PLACE_TRACK_NOT_FOUND
                ));
        boolean userLike = placeTrackLikeRepository.existsById(
                new PlaceTrackLikeId(placeTrackId, memberId)
        );

        return PlaceTrackResponse.PlaceTrackDetail.from(placeTrack, userLike);
    }

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
