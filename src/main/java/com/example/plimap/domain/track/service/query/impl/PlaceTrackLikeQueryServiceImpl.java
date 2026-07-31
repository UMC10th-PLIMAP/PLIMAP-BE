package com.example.plimap.domain.track.service.query.impl;

import com.example.plimap.domain.track.repository.PlaceTrackLikeRepository;
import com.example.plimap.domain.track.service.query.PlaceTrackLikeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceTrackLikeQueryServiceImpl implements PlaceTrackLikeQueryService {

    private final PlaceTrackLikeRepository placeTrackLikeRepository;

    @Override
    public boolean existsActivePlaceTrackLikedByMemberAtPlace(
            Long memberId,
            Long placeId
    ) {
        return placeTrackLikeRepository
                .existsByIdMemberIdAndPlaceTrackPlaceIdAndPlaceTrackDeletedAtIsNull(
                        memberId,
                        placeId
                );
    }
}
