package com.example.plimap.domain.track.service.command.impl;

import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.PlaceTrackLike;
import com.example.plimap.domain.track.entity.PlaceTrackLikeId;
import com.example.plimap.domain.track.exception.TrackErrorCode;
import com.example.plimap.domain.track.exception.TrackException;
import com.example.plimap.domain.track.repository.PlaceTrackLikeRepository;
import com.example.plimap.domain.track.repository.PlaceTrackRepository;
import com.example.plimap.domain.track.service.command.PlaceTrackCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PlaceTrackCommandServiceImpl implements PlaceTrackCommandService {

    private final PlaceTrackRepository placeTrackRepository;
    private final PlaceTrackLikeRepository placeTrackLikeRepository;

    @Override
    public PlaceTrackResponse.PlaceTrackLikeResult createPlaceTrackLike(
            Long memberId,
            Long placeTrackId
    ) {
        PlaceTrack placeTrack = getActivePlaceTrackForUpdate(placeTrackId);
        PlaceTrackLikeId likeId = new PlaceTrackLikeId(placeTrackId, memberId);

        if (placeTrackLikeRepository.existsById(likeId)) {
            throw new TrackException(TrackErrorCode.PLACE_TRACK_ALREADY_LIKED);
        }

        placeTrackLikeRepository.save(PlaceTrackLike.create(placeTrack, memberId));
        placeTrack.increaseLikeCount();
        return PlaceTrackResponse.PlaceTrackLikeResult.from(placeTrack, true);
    }

    @Override
    public PlaceTrackResponse.PlaceTrackLikeResult deletePlaceTrackLike(
            Long memberId,
            Long placeTrackId
    ) {
        PlaceTrack placeTrack = getActivePlaceTrackForUpdate(placeTrackId);
        PlaceTrackLikeId likeId = new PlaceTrackLikeId(placeTrackId, memberId);
        PlaceTrackLike placeTrackLike = placeTrackLikeRepository.findById(likeId)
                .orElseThrow(() -> new TrackException(
                        TrackErrorCode.PLACE_TRACK_LIKE_NOT_FOUND
                ));

        placeTrackLikeRepository.delete(placeTrackLike);
        placeTrack.decreaseLikeCount();
        return PlaceTrackResponse.PlaceTrackLikeResult.from(placeTrack, false);
    }

    private PlaceTrack getActivePlaceTrackForUpdate(Long placeTrackId) {
        return placeTrackRepository.findActiveByIdForUpdate(placeTrackId)
                .orElseThrow(() -> new TrackException(
                        TrackErrorCode.PLACE_TRACK_NOT_FOUND
                ));
    }
}
