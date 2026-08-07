package com.example.plimap.domain.track.service.query.impl;

import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.exception.TrackErrorCode;
import com.example.plimap.domain.track.exception.TrackException;
import com.example.plimap.domain.track.repository.PlaceTrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceTrackFinder {

    private final PlaceTrackRepository placeTrackRepository;

    public PlaceTrack getActivePlaceTrack(Long placeTrackId) {
        return placeTrackRepository.findWithPlaceById(placeTrackId)
                .orElseThrow(() -> new TrackException(TrackErrorCode.PLACE_TRACK_NOT_FOUND));
    }
}
