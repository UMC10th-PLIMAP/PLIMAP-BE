package com.example.plimap.domain.track.service.command.impl;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.track.dto.SelectedTrackCache;
import com.example.plimap.domain.track.dto.request.TrackCommand;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.Track;
import com.example.plimap.domain.track.exception.TrackErrorCode;
import com.example.plimap.domain.track.exception.TrackException;
import com.example.plimap.domain.track.repository.PlaceTrackRepository;
import com.example.plimap.domain.track.repository.TrackRepository;
import com.example.plimap.domain.track.service.command.AlbumImageUrlResolver;
import com.example.plimap.domain.track.service.command.TrackCommandService;
import com.example.plimap.domain.track.service.query.SelectedTrackCacheReader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TrackCommandServiceImpl implements TrackCommandService {

    private static final String YOUTUBE_PROVIDER = "YOUTUBE";

    private final TrackRepository trackRepository;
    private final PlaceTrackRepository placeTrackRepository;
    private final ObjectProvider<SelectedTrackCacheReader> selectedTrackCacheReaderProvider;
    private final AlbumImageUrlResolver albumImageUrlResolver;

    @Override
    @Transactional
    public PlaceTrack getOrCreatePlaceTrack(Place place, TrackCommand.Create command) {
        Objects.requireNonNull(place, "place must not be null");
        Objects.requireNonNull(place.getId(), "place must be persisted");
        Objects.requireNonNull(command, "command must not be null");

        SelectedTrackCache selectedTrack = getSelectedTrackCacheReader()
                .findByItunesTrackId(command.itunesTrackId())
                .orElseThrow(() -> new TrackException(TrackErrorCode.SELECTED_TRACK_CACHE_NOT_FOUND));
        String youtubeVideoId = getYoutubeVideoId(selectedTrack);

        Track track = getOrCreateTrack(selectedTrack, youtubeVideoId);
        return getOrCreatePlaceTrack(place, track);
    }

    private SelectedTrackCacheReader getSelectedTrackCacheReader() {
        SelectedTrackCacheReader cacheReader = selectedTrackCacheReaderProvider.getIfAvailable();
        if (cacheReader == null) {
            throw new TrackException(TrackErrorCode.SELECTED_TRACK_CACHE_READER_NOT_AVAILABLE);
        }
        return cacheReader;
    }

    private Track getOrCreateTrack(SelectedTrackCache selectedTrack, String youtubeVideoId) {
        return trackRepository
                .findByProviderAndProviderTrackId(YOUTUBE_PROVIDER, youtubeVideoId)
                .orElseGet(() -> createTrack(selectedTrack, youtubeVideoId));
    }

    private Track createTrack(SelectedTrackCache selectedTrack, String youtubeVideoId) {
        String albumImageUrl = albumImageUrlResolver.resolve(selectedTrack.albumImageUrl());
        return trackRepository.save(Track.create(
                YOUTUBE_PROVIDER,
                youtubeVideoId,
                selectedTrack.title(),
                selectedTrack.artistName(),
                selectedTrack.albumTitle(),
                albumImageUrl,
                selectedTrack.previewUrl(),
                selectedTrack.durationMs()
        ));
    }

    private String getYoutubeVideoId(SelectedTrackCache selectedTrack) {
        if (selectedTrack.youtubeVideoId() == null || selectedTrack.youtubeVideoId().isBlank()) {
            throw new TrackException(TrackErrorCode.YOUTUBE_VIDEO_ID_NOT_FOUND);
        }
        return selectedTrack.youtubeVideoId();
    }

    private PlaceTrack getOrCreatePlaceTrack(Place place, Track track) {
        PlaceTrack activePlaceTrack = placeTrackRepository
                .findByPlace_IdAndTrack_IdAndDeletedAtIsNull(place.getId(), track.getId())
                .orElse(null);

        if (activePlaceTrack != null) {
            return activePlaceTrack;
        }

        PlaceTrack deletedPlaceTrack = placeTrackRepository
                .findFirstByPlace_IdAndTrack_IdAndDeletedAtIsNotNullOrderByIdDesc(
                        place.getId(),
                        track.getId()
                )
                .orElse(null);

        if (deletedPlaceTrack != null) {
            deletedPlaceTrack.restore();
            return deletedPlaceTrack;
        }

        return placeTrackRepository.save(PlaceTrack.create(place, track));
    }
}
