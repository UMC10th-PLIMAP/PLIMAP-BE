package com.example.plimap.domain.track.service.command.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.plimap.domain.track.dto.PlaybackFailureCache;
import com.example.plimap.domain.track.dto.request.TrackRequest;
import com.example.plimap.domain.track.enums.YoutubePlaybackFailureType;
import com.example.plimap.domain.track.repository.PlaybackFailureCacheRepository;
import org.junit.jupiter.api.Test;

class TrackPlaybackFailureServiceImplTest {

    private final PlaybackFailureCacheRepository repository =
            mock(PlaybackFailureCacheRepository.class);
    private final TrackPlaybackFailureServiceImpl service =
            new TrackPlaybackFailureServiceImpl(repository);

    @Test
    void 재생_불가_오류는_실패_캐시에_저장한다() {
        service.report(request(101));

        verify(repository).save(PlaybackFailureCache.create(
                123L,
                "BzYnNdJhZQw",
                101,
                YoutubePlaybackFailureType.EMBED_BLOCKED
        ));
    }

    @Test
    void 확정적이지_않은_오류는_실패_캐시에_저장하지_않는다() {
        service.report(request(5));

        verifyNoInteractions(repository);
    }

    @Test
    void 실패_보고는_캐시_저장까지만_수행한다() {
        service.report(request(100));

        verify(repository).save(PlaybackFailureCache.create(
                123L,
                "BzYnNdJhZQw",
                100,
                YoutubePlaybackFailureType.VIDEO_UNAVAILABLE
        ));
    }

    private TrackRequest.PlaybackFailure request(int errorCode) {
        return new TrackRequest.PlaybackFailure(123L, "BzYnNdJhZQw", errorCode);
    }
}
