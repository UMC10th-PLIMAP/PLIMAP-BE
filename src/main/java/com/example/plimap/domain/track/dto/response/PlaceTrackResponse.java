package com.example.plimap.domain.track.dto.response;

import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.Track;
import java.util.List;

public final class PlaceTrackResponse {

    private PlaceTrackResponse() {
    }

    public record PlaceTrackListResult(
            Long placeId,
            double distance,
            boolean isWithinRadius,
            Boolean isTrackDetailAccessible,
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
            Integer pinCount,
            Integer likeCount,
            Boolean isLiked,
            Boolean pinByMe
    ) {
    }

    public record PlaceTrackDetail(
            Long placeTrackId,
            Long trackId,
            String youtubeVideoId,
            String title,
            String artist,
            String albumImageUrl,
            Integer likeCount,
            Boolean userLike
    ) {

        public static PlaceTrackDetail from(PlaceTrack placeTrack, boolean userLike) {
            Track track = placeTrack.getTrack();
            return new PlaceTrackDetail(
                    placeTrack.getId(),
                    track.getId(),
                    track.getProviderTrackId(),
                    track.getTitle(),
                    track.getArtistName(),
                    track.getAlbumImageUrl(),
                    placeTrack.getLikeCount(),
                    userLike
            );
        }
    }

    public record PlaceTrackLikeResult(
            Long placeTrackId,
            Boolean isLiked,
            Integer likeCount
    ) {

        public static PlaceTrackLikeResult from(PlaceTrack placeTrack, boolean isLiked) {
            return new PlaceTrackLikeResult(
                    placeTrack.getId(),
                    isLiked,
                    placeTrack.getLikeCount()
            );
        }
    }

    public record LikedPlaceTrackListResult(
            List<LikedPlaceTrackItem> tracks,
            int page,
            int size,
            boolean hasNext
    ) {

        public LikedPlaceTrackListResult {
            tracks = List.copyOf(tracks);
        }
    }

    public record LikedPlaceTrackItem(
            Long placeTrackId,
            String trackName,
            String artistName,
            String artworkUrl,
            Integer likeCount
    ) {
    }
}
