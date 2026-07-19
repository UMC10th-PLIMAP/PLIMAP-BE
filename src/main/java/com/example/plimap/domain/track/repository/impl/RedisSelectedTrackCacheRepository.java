package com.example.plimap.domain.track.repository.impl;

import com.example.plimap.domain.track.dto.SelectedTrackCache;
import com.example.plimap.domain.track.repository.SelectedTrackCacheRepository;
import com.example.plimap.domain.track.repository.exception.CacheSerializationException;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class RedisSelectedTrackCacheRepository implements SelectedTrackCacheRepository {

    static final Duration TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "track:selection:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<SelectedTrackCache> findByItunesTrackId(Long itunesTrackId) {
        String value = redisTemplate.opsForValue().get(key(itunesTrackId));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, SelectedTrackCache.class));
        } catch (JacksonException exception) {
            throw new CacheSerializationException(
                    "선택 트랙 캐시 역직렬화에 실패했습니다.",
                    exception
            );
        }
    }

    @Override
    public void save(SelectedTrackCache selectedTrack) {
        String value;
        try {
            value = objectMapper.writeValueAsString(selectedTrack);
        } catch (JacksonException exception) {
            throw new CacheSerializationException(
                    "선택 트랙 캐시 직렬화에 실패했습니다.",
                    exception
            );
        }
        redisTemplate.opsForValue().set(key(selectedTrack.itunesTrackId()), value, TTL);
    }

    private String key(Long itunesTrackId) {
        return KEY_PREFIX + itunesTrackId;
    }
}
