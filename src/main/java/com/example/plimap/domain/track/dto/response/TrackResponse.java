package com.example.plimap.domain.track.dto.response;

import com.example.plimap.domain.track.dto.SelectedTrackCache;
import com.example.plimap.global.external.itunes.dto.ItunesSearchResponse;
import java.util.List;
import java.util.Set;

public final class TrackResponse {

    private TrackResponse() {
    }

    public record TrackSearchResult(List<TrackSearchItem> tracks) {

        public TrackSearchResult {
            tracks = List.copyOf(tracks);
        }

        public static TrackSearchResult from(ItunesSearchResponse response) {
            return new TrackSearchResult(response.results().stream()
                    .map(TrackSearchItem::from)
                    .toList());
        }

        public TrackSearchResult withUnavailable(Set<Long> unavailableTrackIds) {
            return new TrackSearchResult(tracks.stream()
                    .map(track -> track.withUnavailable(
                            unavailableTrackIds.contains(track.itunesTrackId())
                    ))
                    .toList());
        }
    }

    public record TrackSearchItem(
            Long itunesTrackId,
            String trackName,
            String artistName,
            String albumName,
            String artworkUrl,
            String previewUrl,
            Integer durationMs,
            boolean isUnavailable
    ) {

        public TrackSearchItem(
                Long itunesTrackId,
                String trackName,
                String artistName,
                String albumName,
                String artworkUrl,
                String previewUrl,
                Integer durationMs
        ) {
            this(
                    itunesTrackId,
                    trackName,
                    artistName,
                    albumName,
                    artworkUrl,
                    previewUrl,
                    durationMs,
                    false
            );
        }

        public static TrackSearchItem from(ItunesSearchResponse.Item item) {
            return new TrackSearchItem(
                    item.trackId(),
                    item.trackName(),
                    item.artistName(),
                    item.collectionName(),
                    item.artworkUrl100(),
                    item.previewUrl(),
                    item.trackTimeMillis(),
                    false
            );
        }

        public TrackSearchItem withUnavailable(boolean unavailable) {
            return new TrackSearchItem(
                    itunesTrackId,
                    trackName,
                    artistName,
                    albumName,
                    artworkUrl,
                    previewUrl,
                    durationMs,
                    unavailable
            );
        }
    }

    public record PlaybackPreparationResult(
            Long itunesTrackId,
            String youtubeVideoId,
            String title,
            String artistName,
            String albumTitle,
            String albumImageUrl,
            String previewUrl,
            Integer durationMs
    ) {

        public static PlaybackPreparationResult from(SelectedTrackCache selectedTrack) {
            return new PlaybackPreparationResult(
                    selectedTrack.itunesTrackId(),
                    selectedTrack.youtubeVideoId(),
                    selectedTrack.title(),
                    selectedTrack.artistName(),
                    selectedTrack.albumTitle(),
                    selectedTrack.albumImageUrl(),
                    selectedTrack.previewUrl(),
                    selectedTrack.durationMs()
            );
        }
    }
}
