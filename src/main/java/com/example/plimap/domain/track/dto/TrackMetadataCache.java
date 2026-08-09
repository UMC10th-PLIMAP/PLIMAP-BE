package com.example.plimap.domain.track.dto;

import com.example.plimap.domain.track.dto.response.TrackResponse;

public record TrackMetadataCache(
        Long itunesTrackId,
        String title,
        String artistName,
        String albumTitle,
        String albumImageUrl,
        String previewUrl,
        Integer durationMs
) {

    public static TrackMetadataCache from(TrackResponse.TrackSearchItem item) {
        return new TrackMetadataCache(
                item.itunesTrackId(),
                item.trackName(),
                item.artistName(),
                item.albumName(),
                item.artworkUrl(),
                item.previewUrl(),
                item.durationMs()
        );
    }
}
