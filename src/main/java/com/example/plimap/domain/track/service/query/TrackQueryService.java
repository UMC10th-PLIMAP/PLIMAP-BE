package com.example.plimap.domain.track.service.query;

import com.example.plimap.domain.track.dto.request.TrackRequest;
import com.example.plimap.domain.track.dto.response.TrackResponse;

public interface TrackQueryService {

    TrackResponse.SearchResult searchTracks(TrackRequest.Search request);
}
