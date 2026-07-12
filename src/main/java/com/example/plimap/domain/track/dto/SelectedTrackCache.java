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
}
