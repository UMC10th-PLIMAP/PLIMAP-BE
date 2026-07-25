package com.example.plimap.domain.track.service.command.impl;

import com.example.plimap.domain.track.dto.SelectedTrackCache;
import com.example.plimap.domain.track.dto.TrackMetadataCache;
import com.example.plimap.domain.track.dto.request.TrackRequest;
import com.example.plimap.domain.track.dto.response.TrackResponse;
import com.example.plimap.domain.track.exception.TrackErrorCode;
import com.example.plimap.domain.track.exception.TrackException;
import com.example.plimap.domain.track.repository.SelectedTrackCacheRepository;
import com.example.plimap.domain.track.repository.TrackMetadataCacheRepository;
import com.example.plimap.domain.track.service.command.TrackPlaybackPreparationService;
import com.example.plimap.global.external.youtube.YoutubeClientException;
import com.example.plimap.global.external.youtube.YoutubeSearchClient;
import com.example.plimap.global.external.youtube.dto.YoutubeSearchResponse;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrackPlaybackPreparationServiceImpl
        implements TrackPlaybackPreparationService {

    private static final int MAX_RESULTS = 5;
    private static final String QUERY_SUFFIX = " official audio";

    private final SelectedTrackCacheRepository selectedTrackCacheRepository;
    private final TrackMetadataCacheRepository trackMetadataCacheRepository;
    private final YoutubeSearchClient youtubeSearchClient;

    @Override
    public TrackResponse.PlaybackPreparationResult prepare(
            TrackRequest.PlaybackPreparation request
    ) {
        Objects.requireNonNull(request, "request must not be null");

        Optional<SelectedTrackCache> cachedSelection = findSelection(request.itunesTrackId());
        if (cachedSelection.isPresent()) {
            return TrackResponse.PlaybackPreparationResult.from(cachedSelection.get());
        }

        TrackMetadataCache metadata = findMetadata(request.itunesTrackId());
        YoutubeSearchResponse youtubeResponse = searchYoutube(metadata);
        String youtubeVideoId = selectVideoId(youtubeResponse);
        SelectedTrackCache selectedTrack = SelectedTrackCache.from(metadata, youtubeVideoId);
        saveSelection(selectedTrack);

        return TrackResponse.PlaybackPreparationResult.from(selectedTrack);
    }

    private Optional<SelectedTrackCache> findSelection(Long itunesTrackId) {
        try {
            return selectedTrackCacheRepository.findByItunesTrackId(itunesTrackId);
        } catch (DataAccessException exception) {
            throw new TrackException(TrackErrorCode.TRACK_CACHE_ERROR, exception);
        }
    }

    private TrackMetadataCache findMetadata(Long itunesTrackId) {
        try {
            return trackMetadataCacheRepository.findByItunesTrackId(itunesTrackId)
                    .orElseThrow(() -> new TrackException(
                            TrackErrorCode.TRACK_METADATA_CACHE_NOT_FOUND
                    ));
        } catch (DataAccessException exception) {
            throw new TrackException(TrackErrorCode.TRACK_CACHE_ERROR, exception);
        }
    }

    private YoutubeSearchResponse searchYoutube(TrackMetadataCache metadata) {
        String query = metadata.title() + " " + metadata.artistName() + QUERY_SUFFIX;
        try {
            return youtubeSearchClient.search(query, MAX_RESULTS);
        } catch (YoutubeClientException exception) {
            throw new TrackException(TrackErrorCode.YOUTUBE_EXTERNAL_API_ERROR, exception);
        }
    }

    private String selectVideoId(YoutubeSearchResponse response) {
        if (response.items().isEmpty()) {
            throw new TrackException(TrackErrorCode.YOUTUBE_MATCH_NOT_FOUND);
        }
        return response.items().stream()
                .map(YoutubeSearchResponse.Item::id)
                .filter(Objects::nonNull)
                .map(YoutubeSearchResponse.Id::videoId)
                .filter(Objects::nonNull)
                .filter(videoId -> !videoId.isBlank())
                .findFirst()
                .orElseThrow(() -> new TrackException(TrackErrorCode.YOUTUBE_MATCH_NOT_FOUND));
    }

    private void saveSelection(SelectedTrackCache selectedTrack) {
        try {
            selectedTrackCacheRepository.save(selectedTrack);
        } catch (DataAccessException exception) {
            throw new TrackException(TrackErrorCode.TRACK_CACHE_ERROR, exception);
        }
    }
}
