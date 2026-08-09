package com.example.plimap.domain.track.service.query;

import com.example.plimap.domain.track.entity.PlaceTrack;

public interface PlaceTrackFinder {
    public PlaceTrack getActivePlaceTrack(Long placeTrackId);
}
