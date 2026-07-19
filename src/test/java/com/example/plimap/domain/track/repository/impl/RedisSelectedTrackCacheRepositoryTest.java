package com.example.plimap.domain.track.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.track.dto.SelectedTrackCache;
import com.example.plimap.domain.track.exception.TrackErrorCode;
import com.example.plimap.domain.track.exception.TrackException;
import com.example.plimap.domain.track.repository.exception.CacheSerializationException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
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
        assertThat(result.orElseThrow().albumImageUrl())
                .isEqualTo("https://image.example/cover.jpg");
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

    @Test
    void 조회할_iTunes_ID가_null이거나_0_이하이면_거부한다() {
        assertThatThrownBy(() -> repository.findByItunesTrackId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("itunesTrackId must not be null");
        assertThatThrownBy(() -> repository.findByItunesTrackId(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("itunesTrackId must be positive");
        assertThatThrownBy(() -> repository.findByItunesTrackId(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("itunesTrackId must be positive");
    }

    @Test
    void selection에_YouTube_ID가_없으면_Track_예외를_발생시킨다() {
        SelectedTrackCache invalidCache = selectedTrack(null, "밤편지", "아이유");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY))
                .thenReturn(objectMapper.writeValueAsString(invalidCache));

        assertTrackError(
                () -> repository.findByItunesTrackId(123L),
                TrackErrorCode.YOUTUBE_VIDEO_ID_NOT_FOUND
        );
    }

    @Test
    void selection의_필수_메타데이터가_유효하지_않으면_Track_예외를_발생시킨다() {
        SelectedTrackCache invalidCache = selectedTrack("abcdefghijk", " ", "아이유");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY))
                .thenReturn(objectMapper.writeValueAsString(invalidCache));

        assertTrackError(
                () -> repository.findByItunesTrackId(123L),
                TrackErrorCode.SELECTED_TRACK_CACHE_INVALID
        );
    }

    @Test
    void selection의_iTunes_ID가_조회_ID와_다르면_Track_예외를_발생시킨다() {
        SelectedTrackCache invalidCache = new SelectedTrackCache(
                456L,
                "abcdefghijk",
                "밤편지",
                "아이유",
                "Palette",
                "https://image.example/cover.jpg",
                "https://audio.example/preview.m4a",
                253000
        );
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY))
                .thenReturn(objectMapper.writeValueAsString(invalidCache));

        assertTrackError(
                () -> repository.findByItunesTrackId(123L),
                TrackErrorCode.SELECTED_TRACK_CACHE_INVALID
        );
    }

    @Test
    void Redis_연결_장애를_cache_miss로_변환하지_않는다() {
        RedisConnectionFailureException redisException =
                new RedisConnectionFailureException("redis unavailable");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenThrow(redisException);

        assertThatThrownBy(() -> repository.findByItunesTrackId(123L))
                .isInstanceOfSatisfying(TrackException.class, exception -> {
                    assertThat(exception.getErrorCode())
                            .isEqualTo(TrackErrorCode.TRACK_CACHE_ERROR);
                    assertThat(exception.getCause()).isSameAs(redisException);
                });
    }

    private void assertTrackError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
            TrackErrorCode errorCode
    ) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(TrackException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }

    private SelectedTrackCache selectedTrack() {
        return selectedTrack("abcdefghijk", "밤편지", "아이유");
    }

    private SelectedTrackCache selectedTrack(
            String youtubeVideoId,
            String title,
            String artistName
    ) {
        return new SelectedTrackCache(
                123L,
                youtubeVideoId,
                title,
                artistName,
                "Palette",
                "https://image.example/cover.jpg",
                "https://audio.example/preview.m4a",
                253000
        );
    }
}
