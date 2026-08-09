package com.example.plimap.domain.track.service.query;

import com.example.plimap.domain.track.dto.request.PlaceTrackRequest;
import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;

public interface PlaceTrackQueryService {

    PlaceTrackResponse.LikedPlaceTrackListResult getLikedPlaceTracks(
            Long memberId,
            int page,
            int size
    );

    PlaceTrackResponse.PlaceTrackDetail getPlaceTrackDetail(
            Long memberId,
            Long placeTrackId,
            PlaceTrackRequest.UserLocation request,
            String token
    );

    PlaceTrackResponse.PlaceTrackListResult getPlaceTracks(
            Long memberId,
            Long placeId,
            PlaceTrackRequest.List request
    );
}
