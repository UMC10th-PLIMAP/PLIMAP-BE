package com.example.plimap.domain.track.dto;

public record SelectedTrackCache(
        Long itunesTrackId,
        String youtubeVideoId,
        String title,
        String artistName,
        String albumTitle,
        String albumImageUrl,
        String previewUrl,
        Integer durationMs
) {

    public static SelectedTrackCache from(
            TrackMetadataCache metadata,
            String youtubeVideoId
    ) {
        return new SelectedTrackCache(
                metadata.itunesTrackId(),
                youtubeVideoId,
                metadata.title(),
                metadata.artistName(),
                metadata.albumTitle(),
                metadata.albumImageUrl(),
                metadata.previewUrl(),
                metadata.durationMs()
        );
    }
}
