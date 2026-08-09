package com.example.plimap.domain.track.service.command.impl;

import com.example.plimap.domain.track.dto.PlaybackFailureCache;
import com.example.plimap.domain.track.dto.SelectedTrackCache;
import com.example.plimap.domain.track.dto.request.TrackRequest;
import com.example.plimap.domain.track.enums.YoutubePlaybackFailureType;
import com.example.plimap.domain.track.exception.TrackErrorCode;
import com.example.plimap.domain.track.exception.TrackException;
import com.example.plimap.domain.track.repository.PlaybackFailureCacheRepository;
import com.example.plimap.domain.track.repository.SelectedTrackCacheRepository;
import com.example.plimap.domain.track.service.command.TrackPlaybackFailureService;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrackPlaybackFailureServiceImpl implements TrackPlaybackFailureService {

    private final PlaybackFailureCacheRepository playbackFailureCacheRepository;
    private final SelectedTrackCacheRepository selectedTrackCacheRepository;

    @Override
    public void report(TrackRequest.PlaybackFailure request) {
        Objects.requireNonNull(request, "request must not be null");

        Optional<SelectedTrackCache> selection = findSelection(request.itunesTrackId());
        if (selection.isEmpty()
                || !Objects.equals(
                        selection.get().youtubeVideoId(),
                        request.youtubeVideoId()
                )) {
            return;
        }

        YoutubePlaybackFailureType failureType =
                YoutubePlaybackFailureType.from(request.errorCode());
        if (!failureType.isCacheable()) {
            return;
        }

        PlaybackFailureCache failure = PlaybackFailureCache.create(
                request.itunesTrackId(),
                request.youtubeVideoId(),
                request.errorCode(),
                failureType
        );
        try {
            playbackFailureCacheRepository.save(failure);
        } catch (DataAccessException exception) {
            throw new TrackException(TrackErrorCode.TRACK_CACHE_ERROR, exception);
        }
    }

    private Optional<SelectedTrackCache> findSelection(Long itunesTrackId) {
        try {
            return selectedTrackCacheRepository.findByItunesTrackId(itunesTrackId);
        } catch (DataAccessException exception) {
            throw new TrackException(TrackErrorCode.TRACK_CACHE_ERROR, exception);
        }
    }
}
