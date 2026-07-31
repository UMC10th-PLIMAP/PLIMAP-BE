package com.example.plimap.domain.track.service.query;

public interface PlaceTrackLikeQueryService {

    boolean existsActivePlaceTrackLikedByMemberAtPlace(
            Long memberId,
            Long placeId
    );
}
