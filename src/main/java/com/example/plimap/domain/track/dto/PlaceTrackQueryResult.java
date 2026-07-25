package com.example.plimap.domain.track.dto;

public record PlaceTrackQueryResult(
        Long placeTrackId,
        String trackName,
        String artistName,
        String artworkUrl,
        int pinCount,
        int likeCount,
        boolean liked
) {
}
