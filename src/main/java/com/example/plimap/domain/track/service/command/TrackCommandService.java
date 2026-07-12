package com.example.plimap.domain.track.service.command;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.track.dto.request.TrackCommand;
import com.example.plimap.domain.track.entity.PlaceTrack;

public interface TrackCommandService {

    PlaceTrack getOrCreatePlaceTrack(Place place, TrackCommand.Create command);
}
