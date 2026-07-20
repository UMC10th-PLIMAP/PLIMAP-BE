package com.example.plimap.domain.track.dto.response;

import com.example.plimap.domain.track.dto.SelectedTrackCache;
import com.example.plimap.global.external.itunes.dto.ItunesSearchResponse;
import java.util.List;

public final class TrackResponse {

    private TrackResponse() {
    }

    public record SearchResult(List<Item> tracks) {

        public SearchResult {
            tracks = List.copyOf(tracks);
        }

        public static SearchResult from(ItunesSearchResponse response) {
            return new SearchResult(response.results().stream()
                    .map(Item::from)
                    .toList());
        }
    }

    public record Item(
            Long itunesTrackId,
            String trackName,
            String artistName,
            String albumName,
            String artworkUrl,
            String previewUrl,
            Integer durationMs
    ) {

        public static Item from(ItunesSearchResponse.Item item) {
            return new Item(
                    item.trackId(),
                    item.trackName(),
                    item.artistName(),
                    item.collectionName(),
                    item.artworkUrl100(),
                    item.previewUrl(),
                    item.trackTimeMillis()
            );
        }
    }

    public record PlaybackPreparation(
            Long itunesTrackId,
            String youtubeVideoId,
            String title,
            String artistName,
            String albumTitle,
            String albumImageUrl,
            String previewUrl,
            Integer durationMs
    ) {

        public static PlaybackPreparation from(SelectedTrackCache selectedTrack) {
            return new PlaybackPreparation(
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
