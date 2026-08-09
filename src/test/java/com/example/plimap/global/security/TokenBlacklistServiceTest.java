package com.example.plimap.global.security;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenBlacklistServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

    private final TokenBlacklistService tokenBlacklistService = new TokenBlacklistService(redisTemplate);

    @Test
    void 남은_유효기간만큼_TTL을_설정해_블랙리스트에_등록한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenBlacklistService.blacklist("jti-1", Duration.ofMinutes(10));

        verify(valueOperations).set(eq("blacklist:token:jti-1"), any(), eq(Duration.ofMinutes(10)));
    }

    @Test
    void 이미_만료된_토큰은_블랙리스트에_등록하지_않는다() {
        tokenBlacklistService.blacklist("jti-2", Duration.ZERO);
        tokenBlacklistService.blacklist("jti-3", Duration.ofSeconds(-1));

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void 블랙리스트에_등록된_JTI는_true를_반환한다() {
        when(redisTemplate.hasKey("blacklist:token:jti-4")).thenReturn(true);

        assertThat(tokenBlacklistService.isBlacklisted("jti-4")).isTrue();
    }

    @Test
    void 블랙리스트에_없는_JTI는_false를_반환한다() {
        when(redisTemplate.hasKey("blacklist:token:jti-5")).thenReturn(false);

        assertThat(tokenBlacklistService.isBlacklisted("jti-5")).isFalse();
    }
}
