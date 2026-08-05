package com.example.plimap.domain.track.service.command;

import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;

public interface PlaceTrackCommandService {

    PlaceTrackResponse.PlaceTrackLikeResult createPlaceTrackLike(
            Long memberId,
            Long placeTrackId
    );

    PlaceTrackResponse.PlaceTrackLikeResult deletePlaceTrackLike(
            Long memberId,
            Long placeTrackId
    );

    void hardDeleteLikesByMember(Long memberId);
}
