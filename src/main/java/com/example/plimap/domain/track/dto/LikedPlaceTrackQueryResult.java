package com.example.plimap.domain.track.dto;

public record LikedPlaceTrackQueryResult(
        Long placeTrackId,
        String trackName,
        String artistName,
        String artworkUrl,
        int likeCount
) {
}
