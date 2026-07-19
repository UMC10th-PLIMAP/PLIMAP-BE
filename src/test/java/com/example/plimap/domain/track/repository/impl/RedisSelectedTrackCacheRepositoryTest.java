package com.example.plimap.domain.track.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.track.dto.SelectedTrackCache;
import com.example.plimap.domain.track.repository.exception.CacheSerializationException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

class RedisSelectedTrackCacheRepositoryTest {

    private static final String KEY = "track:selection:123";

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RedisSelectedTrackCacheRepository repository =
            new RedisSelectedTrackCacheRepository(redisTemplate, objectMapper);

    @Test
    void selection을_정확한_키와_24시간_TTL로_JSON_저장한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        repository.save(selectedTrack());

        verify(valueOperations).set(eq(KEY), valueCaptor.capture(), eq(Duration.ofHours(24)));
        assertThat(KEY).doesNotContain("track:selected:");
        assertThat(objectMapper.readValue(valueCaptor.getValue(), SelectedTrackCache.class))
                .isEqualTo(selectedTrack());
    }

    @Test
    void selection을_iTunes_ID_키로_조회하고_JSON을_역직렬화한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY))
                .thenReturn(objectMapper.writeValueAsString(selectedTrack()));

        Optional<SelectedTrackCache> result = repository.findByItunesTrackId(123L);

        assertThat(result).contains(selectedTrack());
        verify(valueOperations).get(KEY);
    }

    @Test
    void selection이_없으면_empty를_반환한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(null);

        assertThat(repository.findByItunesTrackId(123L)).isEmpty();
    }

    @Test
    void selection_직렬화_오류를_CacheSerializationException으로_변환한다() {
        ObjectMapper failingObjectMapper = mock(ObjectMapper.class);
        JacksonException jacksonException = mock(JacksonException.class);
        RedisSelectedTrackCacheRepository failingRepository =
                new RedisSelectedTrackCacheRepository(redisTemplate, failingObjectMapper);
        when(failingObjectMapper.writeValueAsString(any(SelectedTrackCache.class)))
                .thenThrow(jacksonException);

        assertThatThrownBy(() -> failingRepository.save(selectedTrack()))
                .isInstanceOf(CacheSerializationException.class)
                .hasCause(jacksonException);
    }

    @Test
    void selection_역직렬화_오류를_CacheSerializationException으로_변환한다() {
        ObjectMapper failingObjectMapper = mock(ObjectMapper.class);
        JacksonException jacksonException = mock(JacksonException.class);
        RedisSelectedTrackCacheRepository failingRepository =
                new RedisSelectedTrackCacheRepository(redisTemplate, failingObjectMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn("invalid-json");
        when(failingObjectMapper.readValue("invalid-json", SelectedTrackCache.class))
                .thenThrow(jacksonException);

        assertThatThrownBy(() -> failingRepository.findByItunesTrackId(123L))
                .isInstanceOf(CacheSerializationException.class)
                .hasCause(jacksonException);
    }

    private SelectedTrackCache selectedTrack() {
        return new SelectedTrackCache(
                123L,
                "abcdefghijk",
                "밤편지",
                "아이유",
                "Palette",
                "https://image.example/cover.jpg",
                "https://audio.example/preview.m4a",
                253000
        );
    }
}
