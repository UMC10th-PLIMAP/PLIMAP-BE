package com.example.plimap.domain.track.dto.response;

import java.util.List;

public final class PlaceTrackResponse {

    private PlaceTrackResponse() {
    }

    public record PlaceTrackListResult(
            Long placeId,
            double distance,
            boolean isWithinRadius,
            List<PlaceTrackItem> tracks,
            int page,
            int size,
            boolean hasNext
    ) {

        public PlaceTrackListResult {
            tracks = List.copyOf(tracks);
        }
    }

    public record PlaceTrackItem(
            Long placeTrackId,
            String trackName,
            String artistName,
            String artworkUrl,
            int pinCount,
            Integer likeCount,
            Boolean isLiked
    ) {
    }
}
