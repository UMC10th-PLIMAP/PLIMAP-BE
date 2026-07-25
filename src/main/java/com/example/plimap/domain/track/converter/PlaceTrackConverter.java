package com.example.plimap.domain.track.converter;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.track.dto.PlaceTrackQueryResult;
import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;
import java.util.List;
import org.springframework.data.domain.Slice;

public final class PlaceTrackConverter {

    private PlaceTrackConverter() {
    }

    public static PlaceTrackResponse.PlaceTrackListResult toListResult(
            Place place,
            String createdBy,
            double distance,
            boolean withinRadius,
            boolean bookmarked,
            Slice<PlaceTrackQueryResult> placeTracks
    ) {
        List<PlaceTrackResponse.PlaceTrackItem> tracks = placeTracks.getContent().stream()
                .map(placeTrack -> toItem(placeTrack, withinRadius))
                .toList();

        return new PlaceTrackResponse.PlaceTrackListResult(
                place.getId(),
                place.getName(),
                createdBy,
                distance,
                withinRadius,
                bookmarked,
                tracks,
                placeTracks.getNumber(),
                placeTracks.getSize(),
                placeTracks.hasNext()
        );
    }

    private static PlaceTrackResponse.PlaceTrackItem toItem(
            PlaceTrackQueryResult placeTrack,
            boolean withinRadius
    ) {
        return new PlaceTrackResponse.PlaceTrackItem(
                placeTrack.placeTrackId(),
                placeTrack.trackName(),
                placeTrack.artistName(),
                placeTrack.artworkUrl(),
                placeTrack.pinCount(),
                withinRadius ? placeTrack.likeCount() : null,
                withinRadius ? placeTrack.liked() : null
        );
    }
}
