package com.example.plimap.domain.track.dto.response;

import java.util.List;

public final class PlaceTrackResponse {

    private PlaceTrackResponse() {
    }

    public record ListResult(
            Long placeId,
            String placeName,
            String createdBy,
            double distance,
            boolean isWithinRadius,
            boolean isBookmarked,
            List<Item> tracks,
            int page,
            int size,
            boolean hasNext
    ) {

        public ListResult {
            tracks = List.copyOf(tracks);
        }
    }

    public record Item(
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
