package com.example.plimap.domain.track.service.command;

import com.example.plimap.domain.track.dto.request.TrackRequest;
import com.example.plimap.domain.track.dto.response.TrackResponse;

public interface TrackPlaybackPreparationService {

    TrackResponse.PlaybackPreparation prepare(TrackRequest.PlaybackPreparation request);
}
