package com.example.plimap.domain.track.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.track.dto.PlaybackFailureCache;
import com.example.plimap.domain.track.enums.YoutubePlaybackFailureType;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

class RedisPlaybackFailureCacheRepositoryTest {

    private static final String KEY = "track:playback-failure:123";

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RedisPlaybackFailureCacheRepository repository =
            new RedisPlaybackFailureCacheRepository(redisTemplate, objectMapper);

    @Test
    void 재생_실패를_정확한_키와_24시간_TTL로_저장한다() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        // when
        repository.save(failure());

        // then
        verify(valueOperations).set(eq(KEY), valueCaptor.capture(), eq(Duration.ofHours(24)));
        assertThat(objectMapper.readValue(valueCaptor.getValue(), PlaybackFailureCache.class))
                .isEqualTo(failure());
    }

    @Test
    void 재생_실패를_저장하고_단건_조회한다() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(objectMapper.writeValueAsString(failure()));

        // when
        Optional<PlaybackFailureCache> result = repository.findByItunesTrackId(123L);

        // then
        assertThat(result).contains(failure());
    }

    @Test
    void 여러_트랙의_실패_캐시는_multiGet으로_한번에_조회한다() {
        // given
        PlaybackFailureCache secondFailure = PlaybackFailureCache.create(
                456L,
                "secondVideo",
                100,
                YoutubePlaybackFailureType.VIDEO_UNAVAILABLE
        );
        List<String> keys = List.of(KEY, "track:playback-failure:456");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(keys)).thenReturn(List.of(
                objectMapper.writeValueAsString(failure()),
                objectMapper.writeValueAsString(secondFailure)
        ));

        // when
        Map<Long, PlaybackFailureCache> result =
                repository.findAllByItunesTrackIds(List.of(123L, 456L));

        // then
        assertThat(result).containsEntry(123L, failure()).containsEntry(456L, secondFailure);
        verify(valueOperations).multiGet(keys);
    }

    private PlaybackFailureCache failure() {
        return PlaybackFailureCache.create(
                123L,
                "BzYnNdJhZQw",
                101,
                YoutubePlaybackFailureType.EMBED_BLOCKED
        );
    }
}
