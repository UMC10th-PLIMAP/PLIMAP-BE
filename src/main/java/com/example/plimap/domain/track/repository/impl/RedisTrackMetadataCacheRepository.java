package com.example.plimap.domain.track.repository.impl;

import com.example.plimap.domain.track.dto.TrackMetadataCache;
import com.example.plimap.domain.track.repository.TrackMetadataCacheRepository;
import com.example.plimap.domain.track.repository.exception.CacheSerializationException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

@Repository
@RequiredArgsConstructor
public class RedisTrackMetadataCacheRepository implements TrackMetadataCacheRepository {

    static final Duration TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "track:metadata:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(TrackMetadataCache metadata) {
        String key = KEY_PREFIX + metadata.itunesTrackId();
        String value;
        try {
            value = objectMapper.writeValueAsString(metadata);
        } catch (JacksonException exception) {
            throw new CacheSerializationException("트랙 메타데이터 캐시 직렬화에 실패했습니다.", exception);
        }
        redisTemplate.opsForValue().set(key, value, TTL);
    }
}
