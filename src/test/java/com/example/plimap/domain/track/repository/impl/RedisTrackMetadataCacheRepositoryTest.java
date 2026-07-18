package com.example.plimap.domain.track.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.track.dto.TrackMetadataCache;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

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
}
