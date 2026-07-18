package com.example.plimap.domain.track.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.track.dto.TrackMetadataCache;
import com.example.plimap.domain.track.repository.exception.CacheSerializationException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

class RedisTrackMetadataCacheRepositoryTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RedisTrackMetadataCacheRepository repository =
            new RedisTrackMetadataCacheRepository(redisTemplate, objectMapper);

    @Test
    void 곡_메타데이터를_iTunes_ID_키로_24시간_저장한다() {
        TrackMetadataCache metadata = new TrackMetadataCache(
                123L,
                "밤편지",
                "아이유",
                "Palette",
                "https://image.example/cover.jpg",
                "https://audio.example/preview.m4a",
                253000
        );
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        repository.save(metadata);

        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq("track:metadata:123"),
                valueCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(Duration.ofHours(24))
        );
        TrackMetadataCache stored =
                objectMapper.readValue(valueCaptor.getValue(), TrackMetadataCache.class);
        assertThat(stored).isEqualTo(metadata);
    }

    @Test
    void 메타데이터_직렬화_오류를_DataAccessException으로_변환한다() {
        ObjectMapper failingObjectMapper = mock(ObjectMapper.class);
        JacksonException jacksonException = mock(JacksonException.class);
        RedisTrackMetadataCacheRepository failingRepository =
                new RedisTrackMetadataCacheRepository(redisTemplate, failingObjectMapper);
        when(failingObjectMapper.writeValueAsString(any(TrackMetadataCache.class)))
                .thenThrow(jacksonException);

        assertThatThrownBy(() -> failingRepository.save(metadata()))
                .isInstanceOf(CacheSerializationException.class)
                .hasCause(jacksonException);
    }

    private TrackMetadataCache metadata() {
        return new TrackMetadataCache(
                123L,
                "밤편지",
                "아이유",
                "Palette",
                "https://image.example/cover.jpg",
                "https://audio.example/preview.m4a",
                253000
        );
    }
}
