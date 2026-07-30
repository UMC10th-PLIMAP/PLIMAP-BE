package com.example.plimap.domain.track.converter;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.track.dto.LikedPlaceTrackQueryResult;
import com.example.plimap.domain.track.dto.PlaceTrackQueryResult;
import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;
import java.util.List;
import org.springframework.data.domain.Slice;

public final class PlaceTrackConverter {

    private PlaceTrackConverter() {
    }

    public static PlaceTrackResponse.LikedPlaceTrackListResult toLikedListResult(
            Slice<LikedPlaceTrackQueryResult> placeTracks
    ) {
        List<PlaceTrackResponse.LikedPlaceTrackItem> tracks =
                placeTracks.getContent().stream()
                        .map(placeTrack ->
                                new PlaceTrackResponse.LikedPlaceTrackItem(
                                        placeTrack.placeTrackId(),
                                        placeTrack.trackName(),
                                        placeTrack.artistName(),
                                        placeTrack.artworkUrl(),
                                        placeTrack.likeCount()
                                ))
                        .toList();

        return new PlaceTrackResponse.LikedPlaceTrackListResult(
                tracks,
                placeTracks.getNumber(),
                placeTracks.getSize(),
                placeTracks.hasNext()
        );
    }

    public static PlaceTrackResponse.PlaceTrackListResult toListResult(
            Place place,
            double distance,
            boolean withinRadius,
            boolean trackDetailAccessible,
            Slice<PlaceTrackQueryResult> placeTracks
    ) {
        List<PlaceTrackResponse.PlaceTrackItem> tracks = placeTracks.getContent().stream()
                .map(placeTrack -> toItem(
                        placeTrack,
                        trackDetailAccessible
                ))
                .toList();

        return new PlaceTrackResponse.PlaceTrackListResult(
                place.getId(),
                distance,
                withinRadius,
                trackDetailAccessible,
                tracks,
                placeTracks.getNumber(),
                placeTracks.getSize(),
                placeTracks.hasNext()
        );
    }

    private static PlaceTrackResponse.PlaceTrackItem toItem(
            PlaceTrackQueryResult placeTrack,
            boolean trackDetailAccessible
    ) {
        return new PlaceTrackResponse.PlaceTrackItem(
                placeTrack.placeTrackId(),
                placeTrack.trackName(),
                placeTrack.artistName(),
                placeTrack.artworkUrl(),
                placeTrack.pinCount(),
                trackDetailAccessible ? placeTrack.likeCount() : null,
                placeTrack.liked()
        );
    }
}
