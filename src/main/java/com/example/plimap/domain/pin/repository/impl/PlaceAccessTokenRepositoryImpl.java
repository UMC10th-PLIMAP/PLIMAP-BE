package com.example.plimap.domain.pin.repository.impl;

import com.example.plimap.domain.pin.dto.PlaceAccessToken;
import com.example.plimap.domain.pin.repository.PlaceAccessTokenRepository;
import com.example.plimap.domain.track.repository.exception.CacheSerializationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class PlaceAccessTokenRepositoryImpl implements PlaceAccessTokenRepository {

    private static final String KEY_PREFIX = "place:access:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(PlaceAccessToken placeAccessToken, String token) {
        String value;

        try {
            value = objectMapper.writeValueAsString(placeAccessToken);
        } catch (JacksonException exception) {
            throw new CacheSerializationException(
                    "PlaceAccessToken 직렬화에 실패했습니다.",
                    exception
            );
        }

        redisTemplate.opsForValue().set(
                KEY_PREFIX + token,
                value,
                TTL
        );
    }
}
