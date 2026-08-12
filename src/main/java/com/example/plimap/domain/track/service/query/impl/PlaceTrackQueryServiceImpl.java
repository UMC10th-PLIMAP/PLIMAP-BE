package com.example.plimap.domain.track.service.query.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.domain.pin.service.query.PinQueryService;
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
import com.example.plimap.domain.track.service.query.PlaceTrackLikeQueryService;
import java.util.Optional;
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

    private final MemberQueryService memberQueryService;
    private final PinQueryService pinQueryService;
    private final PlaceQueryService placeQueryService;
    private final PinLocationValidator pinLocationValidator;
    private final PlaceTrackRepository placeTrackRepository;
    private final PlaceTrackLikeRepository placeTrackLikeRepository;
    private final PlaceTrackQueryRepository placeTrackQueryRepository;
    private final PlaceTrackLikeQueryService placeTrackLikeQueryService;

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
            Long placeTrackId,
            PlaceTrackRequest.UserLocation request,
            String token
    ) {
        PlaceTrack placeTrack = placeTrackRepository
                .findDetailByIdAndDeletedAtIsNull(placeTrackId)
                .orElseThrow(() -> new TrackException(
                        TrackErrorCode.PLACE_TRACK_NOT_FOUND
                ));
        validatePlaceTrackDetailAccess(memberId, placeTrack, request, token);

        boolean userLike = placeTrackLikeRepository.existsById(
                new PlaceTrackLikeId(placeTrackId, memberId)
        );

        return PlaceTrackResponse.PlaceTrackDetail.from(placeTrack, userLike);
    }

    private void validatePlaceTrackDetailAccess(
            Long memberId,
            PlaceTrack placeTrack,
            PlaceTrackRequest.UserLocation request,
            String token
    ) {
        Place place = placeTrack.getPlace();
        double distance = pinLocationValidator.calculateDistance(
                request.userLatitude(),
                request.userLongitude(),
                place.getLocation().getY(),
                place.getLocation().getX()
        );
        boolean hasDistanceAccess = distance <= ACCESSIBLE_RADIUS_METERS;
        Member member = memberQueryService.getActiveMember(memberId);
        boolean hasMyPinAccess = pinQueryService
                .validatePlacePinAccessByMember(member, place);
        boolean hasLikeAccess = placeTrackLikeQueryService
                .existsActivePlaceTrackLikedByMemberAtPlace(
                        memberId,
                        place.getId()
                );

        if (!hasDistanceAccess && !hasMyPinAccess && !hasLikeAccess
                && !pinQueryService.hasValidFeedToken(
                        token,
                        memberId,
                        place.getId()
                )) {
            throw new TrackException(TrackErrorCode.PLACE_TRACK_ACCESS_DENIED);
        }
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
        boolean trackDetailAccessible = isTrackDetailAccessible(
                memberId,
                placeId,
                place,
                withinRadius
        );
        Pageable pageable = PageRequest.of(request.page(), request.size());

        Slice<PlaceTrackQueryResult> placeTracks =
                placeTrackQueryRepository.findPlaceTracks(
                        placeId,
                        memberId,
                        request.sort(),
                        pageable
                );
        Optional<Long> myPlaceTrackId =
                pinQueryService.findActivePlaceTrackIdByPlaceIdAndMemberId(
                        placeId,
                        memberId
                );
        return PlaceTrackConverter.toListResult(
                place,
                distance,
                withinRadius,
                trackDetailAccessible,
                placeTracks,
                myPlaceTrackId
        );
    }

    private boolean isTrackDetailAccessible(
            Long memberId,
            Long placeId,
            Place place,
            boolean withinRadius
    ) {
        if (withinRadius) {
            return true;
        }

        boolean likedPlaceTrack =
                placeTrackLikeRepository
                        .existsByIdMemberIdAndPlaceTrackPlaceIdAndPlaceTrackDeletedAtIsNull(
                                memberId,
                                placeId
                        );
        if (likedPlaceTrack) {
            return true;
        }

        Member member = memberQueryService.getActiveMember(memberId);
        return Boolean.TRUE.equals(
                pinQueryService.validatePlacePinAccessByMember(member, place)
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
