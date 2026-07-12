package com.example.plimap.domain.track.service.command.impl;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.track.dto.request.TrackCommand;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.Track;
import com.example.plimap.domain.track.repository.PlaceTrackRepository;
import com.example.plimap.domain.track.repository.TrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrackCommandServiceImplTest {

    private final TrackRepository trackRepository = mock(TrackRepository.class);
    private final PlaceTrackRepository placeTrackRepository = mock(PlaceTrackRepository.class);

    private TrackCommandServiceImpl trackCommandService;

    @BeforeEach
    void setUp() {
        trackCommandService = new TrackCommandServiceImpl(
                trackRepository,
                placeTrackRepository
        );
    }

    @Test
    void 활성_PlaceTrack이_존재하면_기존_엔티티를_반환한다() {
        Place place = place(1L);
        Track track = track(2L);
        PlaceTrack placeTrack = mock(PlaceTrack.class);

        when(trackRepository.findByProviderAndProviderTrackId("YOUTUBE", "video-id"))
                .thenReturn(Optional.of(track));
        when(placeTrackRepository.findByPlace_IdAndTrack_IdAndDeletedAtIsNull(1L, 2L))
                .thenReturn(Optional.of(placeTrack));

        PlaceTrack result = trackCommandService.getOrCreatePlaceTrack(place, command());

        assertThat(result).isSameAs(placeTrack);
        verify(placeTrackRepository, never())
                .findFirstByPlace_IdAndTrack_IdAndDeletedAtIsNotNullOrderByIdDesc(any(), any());
        verify(placeTrackRepository, never()).save(any(PlaceTrack.class));
    }

    @Test
    void 삭제된_PlaceTrack이_존재하면_복구하여_반환한다() {
        Place place = place(1L);
        Track track = track(2L);
        PlaceTrack deletedPlaceTrack = mock(PlaceTrack.class);

        when(trackRepository.findByProviderAndProviderTrackId("YOUTUBE", "video-id"))
                .thenReturn(Optional.of(track));
        when(placeTrackRepository.findByPlace_IdAndTrack_IdAndDeletedAtIsNull(1L, 2L))
                .thenReturn(Optional.empty());
        when(placeTrackRepository.findFirstByPlace_IdAndTrack_IdAndDeletedAtIsNotNullOrderByIdDesc(1L, 2L))
                .thenReturn(Optional.of(deletedPlaceTrack));

        PlaceTrack result = trackCommandService.getOrCreatePlaceTrack(place, command());

        assertThat(result).isSameAs(deletedPlaceTrack);
        verify(deletedPlaceTrack).restore();
        verify(placeTrackRepository, never()).save(any(PlaceTrack.class));
    }

    @Test
    void Track과_PlaceTrack이_없으면_모두_신규_생성한다() {
        Place place = place(1L);
        Track savedTrack = track(2L);
        PlaceTrack savedPlaceTrack = mock(PlaceTrack.class);

        when(trackRepository.findByProviderAndProviderTrackId("YOUTUBE", "video-id"))
                .thenReturn(Optional.empty());
        when(trackRepository.save(any(Track.class))).thenReturn(savedTrack);
        when(placeTrackRepository.findByPlace_IdAndTrack_IdAndDeletedAtIsNull(1L, 2L))
                .thenReturn(Optional.empty());
        when(placeTrackRepository.findFirstByPlace_IdAndTrack_IdAndDeletedAtIsNotNullOrderByIdDesc(1L, 2L))
                .thenReturn(Optional.empty());
        when(placeTrackRepository.save(any(PlaceTrack.class))).thenReturn(savedPlaceTrack);

        PlaceTrack result = trackCommandService.getOrCreatePlaceTrack(place, command());

        assertThat(result).isSameAs(savedPlaceTrack);
        verify(trackRepository).save(any(Track.class));
        verify(placeTrackRepository).save(any(PlaceTrack.class));
    }

    private Place place(Long id) {
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(id);
        return place;
    }

    private Track track(Long id) {
        Track track = mock(Track.class);
        when(track.getId()).thenReturn(id);
        return track;
    }

    private TrackCommand.Create command() {
        return new TrackCommand.Create(
                "YOUTUBE",
                "video-id",
                "title",
                "artist",
                "album",
                "https://example.com/album.jpg",
                "https://example.com/preview",
                180_000
        );
    }

}
