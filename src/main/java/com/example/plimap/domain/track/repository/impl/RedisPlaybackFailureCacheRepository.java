package com.example.plimap.domain.track.repository.impl;

import com.example.plimap.domain.track.dto.PlaybackFailureCache;
import com.example.plimap.domain.track.repository.PlaybackFailureCacheRepository;
import com.example.plimap.domain.track.repository.exception.CacheSerializationException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class RedisPlaybackFailureCacheRepository implements PlaybackFailureCacheRepository {

    static final Duration TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "track:playback-failure:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<PlaybackFailureCache> findByItunesTrackId(Long itunesTrackId) {
        String value = redisTemplate.opsForValue().get(key(itunesTrackId));
        return value == null ? Optional.empty() : Optional.of(deserialize(value));
    }

    @Override
    public Map<Long, PlaybackFailureCache> findAllByItunesTrackIds(
            Collection<Long> itunesTrackIds
    ) {
        List<Long> ids = new ArrayList<>(itunesTrackIds);
        if (ids.isEmpty()) {
            return Map.of();
        }

        List<String> keys = ids.stream().map(this::key).toList();
        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null) {
            return Map.of();
        }

        Map<Long, PlaybackFailureCache> failures = new LinkedHashMap<>();
        for (int index = 0; index < ids.size(); index++) {
            String value = values.get(index);
            if (value != null) {
                failures.put(ids.get(index), deserialize(value));
            }
        }
        return Map.copyOf(failures);
    }

    @Override
    public void save(PlaybackFailureCache playbackFailure) {
        String value;
        try {
            value = objectMapper.writeValueAsString(playbackFailure);
        } catch (JacksonException exception) {
            throw new CacheSerializationException(
                    "재생 실패 캐시 직렬화에 실패했습니다.",
                    exception
            );
        }
        redisTemplate.opsForValue().set(
                key(playbackFailure.itunesTrackId()),
                value,
                TTL
        );
    }

    private PlaybackFailureCache deserialize(String value) {
        try {
            return objectMapper.readValue(value, PlaybackFailureCache.class);
        } catch (JacksonException exception) {
            throw new CacheSerializationException(
                    "재생 실패 캐시 역직렬화에 실패했습니다.",
                    exception
            );
        }
    }

    private String key(Long itunesTrackId) {
        return KEY_PREFIX + itunesTrackId;
    }
}
