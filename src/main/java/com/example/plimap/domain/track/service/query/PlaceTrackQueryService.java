package com.example.plimap.domain.track.service.query;

import com.example.plimap.domain.track.dto.request.PlaceTrackRequest;
import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;

public interface PlaceTrackQueryService {

    PlaceTrackResponse.PlaceTrackListResult getPlaceTracks(
            Long memberId,
            Long placeId,
            PlaceTrackRequest.List request
    );
}
