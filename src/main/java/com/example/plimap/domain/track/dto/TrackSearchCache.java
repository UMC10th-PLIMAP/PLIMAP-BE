package com.example.plimap.domain.track.dto;

import com.example.plimap.domain.track.dto.response.TrackResponse;
import java.util.List;

public record TrackSearchCache(List<TrackMetadataCache> tracks) {

    public TrackSearchCache {
        tracks = List.copyOf(tracks);
    }

    public static TrackSearchCache from(TrackResponse.SearchResult result) {
        return new TrackSearchCache(result.tracks().stream()
                .map(TrackMetadataCache::from)
                .toList());
    }

    public TrackResponse.SearchResult toResponse() {
        return new TrackResponse.SearchResult(tracks.stream()
                .map(track -> new TrackResponse.Item(
                        track.itunesTrackId(),
                        track.title(),
                        track.artistName(),
                        track.albumTitle(),
                        track.albumImageUrl(),
                        track.previewUrl(),
                        track.durationMs()
                ))
                .toList());
    }
}
