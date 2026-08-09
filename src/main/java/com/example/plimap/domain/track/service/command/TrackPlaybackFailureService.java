package com.example.plimap.domain.track.service.command;

import com.example.plimap.domain.track.dto.request.TrackRequest;

public interface TrackPlaybackFailureService {

    void report(TrackRequest.PlaybackFailure request);
}
