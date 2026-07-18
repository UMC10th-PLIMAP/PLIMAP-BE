package com.example.plimap.domain.track.repository.impl;

import com.example.plimap.domain.track.dto.TrackMetadataCache;
import com.example.plimap.domain.track.repository.TrackMetadataCacheRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

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
        String value = objectMapper.writeValueAsString(metadata);
        redisTemplate.opsForValue().set(key, value, TTL);
    }
}
