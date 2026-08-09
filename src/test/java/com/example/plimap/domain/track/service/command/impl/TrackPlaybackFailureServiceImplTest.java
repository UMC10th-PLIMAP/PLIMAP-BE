package com.example.plimap.domain.track.service.command.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.track.dto.PlaybackFailureCache;
import com.example.plimap.domain.track.dto.SelectedTrackCache;
import com.example.plimap.domain.track.dto.request.TrackRequest;
import com.example.plimap.domain.track.enums.YoutubePlaybackFailureType;
import com.example.plimap.domain.track.repository.PlaybackFailureCacheRepository;
import com.example.plimap.domain.track.repository.SelectedTrackCacheRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TrackPlaybackFailureServiceImplTest {

    private static final Long ITUNES_TRACK_ID = 123L;
    private static final String YOUTUBE_VIDEO_ID = "BzYnNdJhZQw";

    private final PlaybackFailureCacheRepository playbackFailureCacheRepository =
            mock(PlaybackFailureCacheRepository.class);
    private final SelectedTrackCacheRepository selectedTrackCacheRepository =
            mock(SelectedTrackCacheRepository.class);
    private final TrackPlaybackFailureServiceImpl service =
            new TrackPlaybackFailureServiceImpl(
                    playbackFailureCacheRepository,
                    selectedTrackCacheRepository
            );

    @ParameterizedTest
    @CsvSource({
            "100, VIDEO_UNAVAILABLE",
            "101, EMBED_BLOCKED",
            "150, EMBED_BLOCKED"
    })
    void selection과_일치하는_재생_불가_오류는_실패_캐시에_저장한다(
            int errorCode,
            YoutubePlaybackFailureType failureType
    ) {
        // given
        when(selectedTrackCacheRepository.findByItunesTrackId(ITUNES_TRACK_ID))
                .thenReturn(Optional.of(selectedTrack(YOUTUBE_VIDEO_ID)));
        TrackRequest.PlaybackFailure request = request(YOUTUBE_VIDEO_ID, errorCode);

        // when
        service.report(request);

        // then
        verify(playbackFailureCacheRepository).save(PlaybackFailureCache.create(
                ITUNES_TRACK_ID,
                YOUTUBE_VIDEO_ID,
                errorCode,
                failureType
        ));
    }

    @Test
    void selection_캐시가_없으면_실패_캐시에_저장하지_않는다() {
        // given
        when(selectedTrackCacheRepository.findByItunesTrackId(ITUNES_TRACK_ID))
                .thenReturn(Optional.empty());
        TrackRequest.PlaybackFailure request = request(YOUTUBE_VIDEO_ID, 101);

        // when
        service.report(request);

        // then
        verify(selectedTrackCacheRepository).findByItunesTrackId(ITUNES_TRACK_ID);
        verifyNoInteractions(playbackFailureCacheRepository);
    }

    @Test
    void selection의_YouTube_ID가_다르면_실패_캐시에_저장하지_않는다() {
        // given
        when(selectedTrackCacheRepository.findByItunesTrackId(ITUNES_TRACK_ID))
                .thenReturn(Optional.of(selectedTrack("differentId")));
        TrackRequest.PlaybackFailure request = request(YOUTUBE_VIDEO_ID, 101);

        // when
        service.report(request);

        // then
        verify(selectedTrackCacheRepository).findByItunesTrackId(ITUNES_TRACK_ID);
        verifyNoInteractions(playbackFailureCacheRepository);
    }

    @Test
    void 확정적이지_않은_오류는_실패_캐시에_저장하지_않는다() {
        // given
        when(selectedTrackCacheRepository.findByItunesTrackId(ITUNES_TRACK_ID))
                .thenReturn(Optional.of(selectedTrack(YOUTUBE_VIDEO_ID)));
        TrackRequest.PlaybackFailure request = request(YOUTUBE_VIDEO_ID, 5);

        // when
        service.report(request);

        // then
        verifyNoInteractions(playbackFailureCacheRepository);
    }

    private TrackRequest.PlaybackFailure request(String youtubeVideoId, int errorCode) {
        return new TrackRequest.PlaybackFailure(
                ITUNES_TRACK_ID,
                youtubeVideoId,
                errorCode
        );
    }

    private SelectedTrackCache selectedTrack(String youtubeVideoId) {
        return new SelectedTrackCache(
                ITUNES_TRACK_ID,
                youtubeVideoId,
                "밤편지",
                "아이유",
                "Palette",
                "https://image.example/cover.jpg",
                "https://audio.example/preview.m4a",
                253000
        );
    }
}
