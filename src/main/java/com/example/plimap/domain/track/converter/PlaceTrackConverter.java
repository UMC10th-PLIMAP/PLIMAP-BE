package com.example.plimap.domain.track.converter;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.track.dto.AlbumImage;
import com.example.plimap.domain.track.dto.LikedPlaceTrackQueryResult;
import com.example.plimap.domain.track.dto.PlaceTrackQueryResult;
import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.Track;
import java.util.List;
import java.util.Optional;
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
            Slice<PlaceTrackQueryResult> placeTracks,
            Optional<Long> myPlaceTrackId
    ) {
        List<PlaceTrackResponse.PlaceTrackItem> tracks = placeTracks.getContent().stream()
                .map(placeTrack -> toItem(
                        placeTrack,
                        trackDetailAccessible,
                        myPlaceTrackId
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
            boolean trackDetailAccessible,
            Optional<Long> myPlaceTrackId
    ) {
        return new PlaceTrackResponse.PlaceTrackItem(
                placeTrack.placeTrackId(),
                placeTrack.trackName(),
                placeTrack.artistName(),
                placeTrack.artworkUrl(),
                placeTrack.pinCount(),
                trackDetailAccessible ? placeTrack.likeCount() : null,
                placeTrack.liked(),
                myPlaceTrackId
                        .map(placeTrack.placeTrackId()::equals)
                        .orElse(false)
        );
    }

    public static AlbumImage toAlbumImage(
            PlaceTrack placeTrack
    ) {
        return AlbumImage.builder()
                .albumImageUrl(placeTrack.getTrack().getAlbumImageUrl())
                .build();
    }
}
