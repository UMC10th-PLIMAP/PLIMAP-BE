package com.example.plimap.global.security;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RefreshTokenServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final RefreshTokenService refreshTokenService = new RefreshTokenService(redisTemplate);

    @Test
    void TTL이_0_또는_음수이면_JTI를_저장하지_않는다() {
        refreshTokenService.save(1L, "refresh-token-jti", Duration.ZERO);
        refreshTokenService.save(1L, "refresh-token-jti", Duration.ofSeconds(-1));

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void TTL이_0_또는_음수이면_JTI를_회전하지_않는다() {
        boolean zeroTtlResult = refreshTokenService.rotateIfMatches(
                1L,
                "current-refresh-jti",
                "new-refresh-jti",
                Duration.ZERO
        );
        boolean negativeTtlResult = refreshTokenService.rotateIfMatches(
                1L,
                "current-refresh-jti",
                "new-refresh-jti",
                Duration.ofSeconds(-1)
        );

        assertThat(zeroTtlResult).isFalse();
        assertThat(negativeTtlResult).isFalse();
        verifyNoInteractions(redisTemplate);
    }
}
