package com.example.plimap.domain.track.service.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.track.dto.SelectedTrackCache;
import com.example.plimap.domain.track.dto.TrackMetadataCache;
import com.example.plimap.domain.track.dto.request.TrackRequest;
import com.example.plimap.domain.track.dto.response.TrackResponse;
import com.example.plimap.domain.track.exception.TrackErrorCode;
import com.example.plimap.domain.track.exception.TrackException;
import com.example.plimap.domain.track.repository.SelectedTrackCacheRepository;
import com.example.plimap.domain.track.repository.TrackMetadataCacheRepository;
import com.example.plimap.global.external.youtube.YoutubeClientException;
import com.example.plimap.global.external.youtube.YoutubeSearchClient;
import com.example.plimap.global.external.youtube.dto.YoutubeSearchResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;

class TrackPlaybackPreparationServiceImplTest {

    private static final Long ITUNES_TRACK_ID = 123L;
    private static final String QUERY = "밤편지 아이유 official audio";

    private final SelectedTrackCacheRepository selectedTrackCacheRepository =
            mock(SelectedTrackCacheRepository.class);
    private final TrackMetadataCacheRepository trackMetadataCacheRepository =
            mock(TrackMetadataCacheRepository.class);
    private final YoutubeSearchClient youtubeSearchClient = mock(YoutubeSearchClient.class);
    private final TrackPlaybackPreparationServiceImpl service =
            new TrackPlaybackPreparationServiceImpl(
                    selectedTrackCacheRepository,
                    trackMetadataCacheRepository,
                    youtubeSearchClient
            );

    @Test
    void selection_캐시가_있으면_외부_API와_metadata_캐시를_조회하지_않는다() {
        when(selectedTrackCacheRepository.findByItunesTrackId(ITUNES_TRACK_ID))
                .thenReturn(Optional.of(selectedTrack()));

        TrackResponse.PlaybackPreparationResult result = service.prepare(request());

        assertThat(result).isEqualTo(response());
        verify(selectedTrackCacheRepository).findByItunesTrackId(ITUNES_TRACK_ID);
        verifyNoMoreInteractions(selectedTrackCacheRepository);
        verifyNoInteractions(trackMetadataCacheRepository, youtubeSearchClient);
    }

    @Test
    void selection_캐시가_없으면_metadata로_YouTube를_검색하고_선택_캐시를_저장한다() {
        when(selectedTrackCacheRepository.findByItunesTrackId(ITUNES_TRACK_ID))
                .thenReturn(Optional.empty());
        when(trackMetadataCacheRepository.findByItunesTrackId(ITUNES_TRACK_ID))
                .thenReturn(Optional.of(metadata()));
        when(youtubeSearchClient.search(QUERY, 5))
                .thenReturn(youtubeResponse("abcdefghijk"));

        TrackResponse.PlaybackPreparationResult result = service.prepare(request());

        assertThat(result).isEqualTo(response());
        verify(youtubeSearchClient).search(QUERY, 5);
        verify(selectedTrackCacheRepository).save(selectedTrack());
    }

    @Test
    void metadata_캐시가_없으면_만료_예외를_발생시킨다() {
        when(selectedTrackCacheRepository.findByItunesTrackId(ITUNES_TRACK_ID))
                .thenReturn(Optional.empty());
        when(trackMetadataCacheRepository.findByItunesTrackId(ITUNES_TRACK_ID))
                .thenReturn(Optional.empty());

        assertTrackError(
                () -> service.prepare(request()),
                TrackErrorCode.TRACK_METADATA_CACHE_NOT_FOUND
        );

        verifyNoInteractions(youtubeSearchClient);
        verify(selectedTrackCacheRepository, never()).save(selectedTrack());
    }

    @Test
    void YouTube_검색_결과가_비어_있으면_매칭_실패_예외를_발생시킨다() {
        givenMetadata();
        when(youtubeSearchClient.search(QUERY, 5))
                .thenReturn(new YoutubeSearchResponse(List.of()));

        assertTrackError(
                () -> service.prepare(request()),
                TrackErrorCode.YOUTUBE_MATCH_NOT_FOUND
        );

        verify(selectedTrackCacheRepository, never()).save(selectedTrack());
    }

    @Test
    void 모든_YouTube_videoId가_null이거나_blank이면_매칭에_실패한다() {
        givenMetadata();
        when(youtubeSearchClient.search(QUERY, 5)).thenReturn(new YoutubeSearchResponse(List.of(
                new YoutubeSearchResponse.Item(null),
                new YoutubeSearchResponse.Item(new YoutubeSearchResponse.Id(null)),
                new YoutubeSearchResponse.Item(new YoutubeSearchResponse.Id("  "))
        )));

        assertTrackError(
                () -> service.prepare(request()),
                TrackErrorCode.YOUTUBE_MATCH_NOT_FOUND
        );

        verify(selectedTrackCacheRepository, never()).save(selectedTrack());
    }

    @Test
    void 첫_결과가_무효이면_두_번째_유효한_videoId를_선택한다() {
        givenMetadata();
        when(youtubeSearchClient.search(QUERY, 5)).thenReturn(new YoutubeSearchResponse(List.of(
                new YoutubeSearchResponse.Item(new YoutubeSearchResponse.Id(" ")),
                new YoutubeSearchResponse.Item(new YoutubeSearchResponse.Id("abcdefghijk"))
        )));

        TrackResponse.PlaybackPreparationResult result = service.prepare(request());

        assertThat(result.youtubeVideoId()).isEqualTo("abcdefghijk");
        verify(selectedTrackCacheRepository).save(selectedTrack());
    }

    @Test
    void YouTube_외부_API_오류를_Track_예외로_변환한다() {
        givenMetadata();
        when(youtubeSearchClient.search(QUERY, 5))
                .thenThrow(new YoutubeClientException("request failed"));

        assertTrackError(
                () -> service.prepare(request()),
                TrackErrorCode.YOUTUBE_EXTERNAL_API_ERROR
        );

        verify(selectedTrackCacheRepository, never()).save(selectedTrack());
    }

    @Test
    void selection_캐시_저장_실패는_캐시_오류로_처리한다() {
        givenMetadata();
        when(youtubeSearchClient.search(QUERY, 5))
                .thenReturn(youtubeResponse("abcdefghijk"));
        doThrow(new RedisConnectionFailureException("redis unavailable"))
                .when(selectedTrackCacheRepository)
                .save(selectedTrack());

        assertTrackError(
                () -> service.prepare(request()),
                TrackErrorCode.TRACK_CACHE_ERROR
        );
    }

    @Test
    void Redis_조회_장애를_metadata_만료로_처리하지_않는다() {
        when(selectedTrackCacheRepository.findByItunesTrackId(ITUNES_TRACK_ID))
                .thenThrow(new RedisConnectionFailureException("redis unavailable"));

        assertTrackError(
                () -> service.prepare(request()),
                TrackErrorCode.TRACK_CACHE_ERROR
        );

        verifyNoInteractions(trackMetadataCacheRepository, youtubeSearchClient);
    }

    private void givenMetadata() {
        when(selectedTrackCacheRepository.findByItunesTrackId(ITUNES_TRACK_ID))
                .thenReturn(Optional.empty());
        when(trackMetadataCacheRepository.findByItunesTrackId(ITUNES_TRACK_ID))
                .thenReturn(Optional.of(metadata()));
    }

    private void assertTrackError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
            TrackErrorCode errorCode
    ) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(TrackException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }

    private TrackRequest.PlaybackPreparation request() {
        return new TrackRequest.PlaybackPreparation(ITUNES_TRACK_ID);
    }

    private TrackMetadataCache metadata() {
        return new TrackMetadataCache(
                ITUNES_TRACK_ID,
                "밤편지",
                "아이유",
                "Palette",
                "https://image.example/cover.jpg",
                "https://audio.example/preview.m4a",
                253000
        );
    }

    private SelectedTrackCache selectedTrack() {
        return new SelectedTrackCache(
                ITUNES_TRACK_ID,
                "abcdefghijk",
                "밤편지",
                "아이유",
                "Palette",
                "https://image.example/cover.jpg",
                "https://audio.example/preview.m4a",
                253000
        );
    }

    private TrackResponse.PlaybackPreparationResult response() {
        return TrackResponse.PlaybackPreparationResult.from(selectedTrack());
    }

    private YoutubeSearchResponse youtubeResponse(String videoId) {
        return new YoutubeSearchResponse(List.of(
                new YoutubeSearchResponse.Item(new YoutubeSearchResponse.Id(videoId))
        ));
    }
}
