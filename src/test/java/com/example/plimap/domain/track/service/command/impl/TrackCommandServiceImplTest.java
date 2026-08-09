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
import com.example.plimap.domain.track.service.query.SelectedTrackCacheReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TrackCommandServiceImplTest {

    private static final Long ITUNES_TRACK_ID = 123456789L;
    private static final String YOUTUBE_VIDEO_ID = "youtube-video-id";

    private final TrackRepository trackRepository = mock(TrackRepository.class);
    private final PlaceTrackRepository placeTrackRepository = mock(PlaceTrackRepository.class);
    private final SelectedTrackCacheReader selectedTrackCacheReader = mock(SelectedTrackCacheReader.class);
    private final ObjectProvider<SelectedTrackCacheReader> selectedTrackCacheReaderProvider = mock(ObjectProvider.class);
    private final AlbumImageUrlResolver albumImageUrlResolver = mock(AlbumImageUrlResolver.class);

    private TrackCommandServiceImpl trackCommandService;

    @BeforeEach
    void setUp() {
        when(selectedTrackCacheReaderProvider.getIfAvailable()).thenReturn(selectedTrackCacheReader);
        trackCommandService = new TrackCommandServiceImpl(
                trackRepository,
                placeTrackRepository,
                selectedTrackCacheReaderProvider,
                albumImageUrlResolver
        );
    }

    @Test
    void iTunes_Track_ID가_null이면_Command_생성을_거부한다() {
        assertThatThrownBy(() -> new TrackCommand.Create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("itunesTrackId must not be null");
    }

    @Test
    void iTunes_Track_ID가_0_이하이면_Command_생성을_거부한다() {
        assertThatThrownBy(() -> new TrackCommand.Create(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("itunesTrackId must be positive");
        assertThatThrownBy(() -> new TrackCommand.Create(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("itunesTrackId must be positive");
    }

    @Test
    void 선택_캐시_Reader가_없으면_예외가_발생한다() {
        when(selectedTrackCacheReaderProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> trackCommandService.getOrCreatePlaceTrack(place(1L), command()))
                .isInstanceOfSatisfying(TrackException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(TrackErrorCode.SELECTED_TRACK_CACHE_READER_NOT_AVAILABLE));

        verifyNoInteractions(selectedTrackCacheReader, trackRepository, placeTrackRepository);
    }

    @Test
    void 선택_캐시와_YouTube_ID로_Track을_조회하고_활성_PlaceTrack을_재사용한다() {
        Place place = place(1L);
        Track track = track(2L);
        PlaceTrack placeTrack = mock(PlaceTrack.class);

        when(selectedTrackCacheReader.findByItunesTrackId(ITUNES_TRACK_ID))
                .thenReturn(Optional.of(selectedTrackCache(YOUTUBE_VIDEO_ID)));
        when(trackRepository.findByProviderAndProviderTrackId("YOUTUBE", YOUTUBE_VIDEO_ID))
                .thenReturn(Optional.of(track));
        when(placeTrackRepository.findByPlace_IdAndTrack_IdAndDeletedAtIsNull(1L, 2L))
                .thenReturn(Optional.of(placeTrack));

        PlaceTrack result = trackCommandService.getOrCreatePlaceTrack(place, command());

        assertThat(result).isSameAs(placeTrack);
        InOrder flow = inOrder(selectedTrackCacheReader, trackRepository);
        flow.verify(selectedTrackCacheReader).findByItunesTrackId(ITUNES_TRACK_ID);
        flow.verify(trackRepository).findByProviderAndProviderTrackId("YOUTUBE", YOUTUBE_VIDEO_ID);
        verify(trackRepository, never()).save(any(Track.class));
        verifyNoInteractions(albumImageUrlResolver);
        verify(placeTrackRepository, never())
                .findFirstByPlace_IdAndTrack_IdAndDeletedAtIsNotNullOrderByIdDesc(any(), any());
        verify(placeTrackRepository, never()).save(any(PlaceTrack.class));
    }

    @Test
    void Track이_없으면_선택_캐시의_메타데이터로_생성하고_PlaceTrack도_생성한다() {
        Place place = place(1L);
        Track savedTrack = track(2L);
        PlaceTrack savedPlaceTrack = mock(PlaceTrack.class);
        ArgumentCaptor<Track> trackCaptor = ArgumentCaptor.forClass(Track.class);

        when(selectedTrackCacheReader.findByItunesTrackId(ITUNES_TRACK_ID))
                .thenReturn(Optional.of(selectedTrackCache(YOUTUBE_VIDEO_ID)));
        when(trackRepository.findByProviderAndProviderTrackId("YOUTUBE", YOUTUBE_VIDEO_ID))
                .thenReturn(Optional.empty());
        when(albumImageUrlResolver.resolve("https://example.com/album.jpg"))
                .thenReturn("https://example.com/album-600.jpg");
        when(trackRepository.save(any(Track.class))).thenReturn(savedTrack);
        when(placeTrackRepository.findByPlace_IdAndTrack_IdAndDeletedAtIsNull(1L, 2L))
                .thenReturn(Optional.empty());
        when(placeTrackRepository.findFirstByPlace_IdAndTrack_IdAndDeletedAtIsNotNullOrderByIdDesc(1L, 2L))
                .thenReturn(Optional.empty());
        when(placeTrackRepository.save(any(PlaceTrack.class))).thenReturn(savedPlaceTrack);

        PlaceTrack result = trackCommandService.getOrCreatePlaceTrack(place, command());

        assertThat(result).isSameAs(savedPlaceTrack);
        verify(trackRepository).save(trackCaptor.capture());
        Track createdTrack = trackCaptor.getValue();
        assertThat(createdTrack.getProvider()).isEqualTo("YOUTUBE");
        assertThat(createdTrack.getProviderTrackId()).isEqualTo(YOUTUBE_VIDEO_ID);
        assertThat(createdTrack.getTitle()).isEqualTo("title");
        assertThat(createdTrack.getArtistName()).isEqualTo("artist");
        assertThat(createdTrack.getAlbumTitle()).isEqualTo("album");
        assertThat(createdTrack.getAlbumImageUrl()).isEqualTo("https://example.com/album-600.jpg");
        assertThat(createdTrack.getPreviewUrl()).isEqualTo("https://example.com/preview");
        assertThat(createdTrack.getDurationMs()).isEqualTo(180_000);
        verify(albumImageUrlResolver).resolve("https://example.com/album.jpg");
        verify(placeTrackRepository).save(any(PlaceTrack.class));
    }

    @Test
    void 선택_캐시가_없으면_예외가_발생한다() {
        when(selectedTrackCacheReader.findByItunesTrackId(ITUNES_TRACK_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackCommandService.getOrCreatePlaceTrack(place(1L), command()))
                .isInstanceOfSatisfying(TrackException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(TrackErrorCode.SELECTED_TRACK_CACHE_NOT_FOUND));

        verifyNoInteractions(trackRepository, placeTrackRepository);
    }

    @Test
    void 선택_캐시에_YouTube_ID가_없으면_예외가_발생한다() {
        when(selectedTrackCacheReader.findByItunesTrackId(ITUNES_TRACK_ID))
                .thenReturn(Optional.of(selectedTrackCache(null)));

        assertThatThrownBy(() -> trackCommandService.getOrCreatePlaceTrack(place(1L), command()))
                .isInstanceOfSatisfying(TrackException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(TrackErrorCode.YOUTUBE_VIDEO_ID_NOT_FOUND));

        verifyNoInteractions(trackRepository, placeTrackRepository);
    }

    @Test
    void 삭제된_PlaceTrack이_존재하면_복구하여_반환한다() {
        Place place = place(1L);
        Track track = track(2L);
        PlaceTrack deletedPlaceTrack = mock(PlaceTrack.class);

        when(selectedTrackCacheReader.findByItunesTrackId(ITUNES_TRACK_ID))
                .thenReturn(Optional.of(selectedTrackCache(YOUTUBE_VIDEO_ID)));
        when(trackRepository.findByProviderAndProviderTrackId("YOUTUBE", YOUTUBE_VIDEO_ID))
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
        return new TrackCommand.Create(ITUNES_TRACK_ID);
    }

    private SelectedTrackCache selectedTrackCache(String youtubeVideoId) {
        return new SelectedTrackCache(
                ITUNES_TRACK_ID,
                youtubeVideoId,
                "title",
                "artist",
                "album",
                "https://example.com/album.jpg",
                "https://example.com/preview",
                180_000
        );
    }
}
