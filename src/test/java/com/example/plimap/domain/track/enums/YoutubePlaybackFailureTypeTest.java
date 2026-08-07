package com.example.plimap.domain.track.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class YoutubePlaybackFailureTypeTest {

    @ParameterizedTest
    @CsvSource({
            "2, INVALID_PARAMETER, false",
            "5, HTML5_PLAYER_ERROR, false",
            "100, VIDEO_UNAVAILABLE, true",
            "101, EMBED_BLOCKED, true",
            "150, EMBED_BLOCKED, true",
            "999, UNKNOWN, false"
    })
    void 오류_코드를_실패_유형과_캐시_저장_여부로_분류한다(
            int errorCode,
            YoutubePlaybackFailureType expectedType,
            boolean cacheable
    ) {
        YoutubePlaybackFailureType result = YoutubePlaybackFailureType.from(errorCode);

        assertThat(result).isEqualTo(expectedType);
        assertThat(result.isCacheable()).isEqualTo(cacheable);
    }
}
