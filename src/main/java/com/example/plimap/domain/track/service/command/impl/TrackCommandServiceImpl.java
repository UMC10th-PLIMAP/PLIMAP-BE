package com.example.plimap.domain.track.service.command.impl;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.track.dto.request.TrackCommand;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.Track;
import com.example.plimap.domain.track.repository.PlaceTrackRepository;
import com.example.plimap.domain.track.repository.TrackRepository;
import com.example.plimap.domain.track.service.command.TrackCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TrackCommandServiceImpl implements TrackCommandService {

    private final TrackRepository trackRepository;
    private final PlaceTrackRepository placeTrackRepository;

    @Override
    @Transactional
    public PlaceTrack getOrCreatePlaceTrack(Place place, TrackCommand.Create command) {
        Objects.requireNonNull(place, "place must not be null");
        Objects.requireNonNull(place.getId(), "place must be persisted");
        Objects.requireNonNull(command, "command must not be null");

        Track track = getOrCreateTrack(command);
        return getOrCreatePlaceTrack(place, track);
    }

    private Track getOrCreateTrack(TrackCommand.Create command) {
        return trackRepository
                .findByProviderAndProviderTrackId(command.provider(), command.providerTrackId())
                .orElseGet(() -> trackRepository.save(Track.create(
                        command.provider(),
                        command.providerTrackId(),
                        command.title(),
                        command.artistName(),
                        command.albumTitle(),
                        command.albumImageUrl(),
                        command.previewUrl(),
                        command.durationMs()
                )));
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
