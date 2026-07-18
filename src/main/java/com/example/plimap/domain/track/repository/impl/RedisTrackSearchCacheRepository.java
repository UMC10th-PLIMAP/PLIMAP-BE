package com.example.plimap.domain.track.repository.impl;

import com.example.plimap.domain.track.dto.TrackSearchCache;
import com.example.plimap.domain.track.repository.TrackSearchCacheRepository;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class RedisTrackSearchCacheRepository implements TrackSearchCacheRepository {

    static final Duration TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "itunes:search:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<TrackSearchCache> find(String normalizedKeyword, int limit) {
        String value = redisTemplate.opsForValue().get(key(normalizedKeyword, limit));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(value, TrackSearchCache.class));
    }

    @Override
    public void save(String normalizedKeyword, int limit, TrackSearchCache searchResult) {
        String value = objectMapper.writeValueAsString(searchResult);
        redisTemplate.opsForValue().set(key(normalizedKeyword, limit), value, TTL);
    }

    private String key(String normalizedKeyword, int limit) {
        return KEY_PREFIX + normalizedKeyword + ":" + limit;
    }
}
