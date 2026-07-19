package com.example.plimap.domain.track.repository.impl;

import com.example.plimap.domain.track.dto.SelectedTrackCache;
import com.example.plimap.domain.track.exception.TrackErrorCode;
import com.example.plimap.domain.track.exception.TrackException;
import com.example.plimap.domain.track.repository.SelectedTrackCacheRepository;
import com.example.plimap.domain.track.repository.exception.CacheSerializationException;
import com.example.plimap.domain.track.service.query.SelectedTrackCacheReader;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class RedisSelectedTrackCacheRepository
        implements SelectedTrackCacheRepository, SelectedTrackCacheReader {

    static final Duration TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "track:selection:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<SelectedTrackCache> findByItunesTrackId(Long itunesTrackId) {
        validateItunesTrackId(itunesTrackId);
        String value;
        try {
            value = redisTemplate.opsForValue().get(key(itunesTrackId));
        } catch (DataAccessException exception) {
            throw new TrackException(TrackErrorCode.TRACK_CACHE_ERROR, exception);
        }
        if (value == null) {
            return Optional.empty();
        }
        try {
            SelectedTrackCache selectedTrack =
                    objectMapper.readValue(value, SelectedTrackCache.class);
            validateSelectedTrack(selectedTrack, itunesTrackId);
            return Optional.of(selectedTrack);
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

    private void validateItunesTrackId(Long itunesTrackId) {
        if (itunesTrackId == null) {
            throw new IllegalArgumentException("itunesTrackId must not be null");
        }
        if (itunesTrackId <= 0) {
            throw new IllegalArgumentException("itunesTrackId must be positive");
        }
    }

    private void validateSelectedTrack(
            SelectedTrackCache selectedTrack,
            Long requestedItunesTrackId
    ) {
        if (selectedTrack == null
                || !requestedItunesTrackId.equals(selectedTrack.itunesTrackId())
                || selectedTrack.title() == null
                || selectedTrack.title().isBlank()
                || selectedTrack.artistName() == null
                || selectedTrack.artistName().isBlank()) {
            throw new TrackException(TrackErrorCode.SELECTED_TRACK_CACHE_INVALID);
        }
        if (selectedTrack.youtubeVideoId() == null
                || selectedTrack.youtubeVideoId().isBlank()) {
            throw new TrackException(TrackErrorCode.YOUTUBE_VIDEO_ID_NOT_FOUND);
        }
    }
}
