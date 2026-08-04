package com.example.plimap.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({PostgisContainerConfiguration.class, RedisContainerConfiguration.class})
class RefreshTokenServiceIntegrationTest {

    private static final Long MEMBER_ID = 900001L;
    private static final String KEY = "refresh:token:" + MEMBER_ID;
    private static final Duration TTL = Duration.ofMinutes(10);

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    @AfterEach
    void 리프레시_토큰을_정리한다() {
        redisTemplate.delete(KEY);
    }

    @Test
    void 리프레시_토큰_원문_대신_JTI를_TTL과_함께_저장한다() {
        refreshTokenService.save(MEMBER_ID, "refresh-token-jti", TTL);

        assertThat(redisTemplate.opsForValue().get(KEY)).isEqualTo("refresh-token-jti");
        assertThat(redisTemplate.getExpire(KEY)).isPositive();
    }

    @Test
    void 현재_JTI가_일치하면_새_JTI로_원자적으로_회전한다() {
        refreshTokenService.save(MEMBER_ID, "current-jti", TTL);

        boolean rotated = refreshTokenService.rotateIfMatches(
                MEMBER_ID,
                "current-jti",
                "new-jti",
                TTL
        );

        assertThat(rotated).isTrue();
        assertThat(redisTemplate.opsForValue().get(KEY)).isEqualTo("new-jti");
        assertThat(redisTemplate.getExpire(KEY)).isPositive();
    }

    @Test
    void 기존_토큰_원문_형식은_JTI_회전에_사용할_수_없다() {
        redisTemplate.opsForValue().set(KEY, "legacy-raw-refresh-token", TTL);

        boolean rotated = refreshTokenService.rotateIfMatches(
                MEMBER_ID,
                "legacy-refresh-jti",
                "new-jti",
                TTL
        );

        assertThat(rotated).isFalse();
        assertThat(redisTemplate.opsForValue().get(KEY)).isEqualTo("legacy-raw-refresh-token");
    }

    @Test
    void 동일한_JTI로_동시에_회전하면_하나의_요청만_성공한다() throws Exception {
        int requestCount = 20;
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        refreshTokenService.save(MEMBER_ID, "shared-current-jti", TTL);

        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        try {
            List<Future<Boolean>> results = IntStream.range(0, requestCount)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        if (!start.await(5, TimeUnit.SECONDS)) {
                            return false;
                        }
                        return refreshTokenService.rotateIfMatches(
                                MEMBER_ID,
                                "shared-current-jti",
                                "new-jti-" + index,
                                TTL
                        );
                    }))
                    .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            long successCount = 0;
            for (Future<Boolean> result : results) {
                if (result.get(5, TimeUnit.SECONDS)) {
                    successCount++;
                }
            }

            assertThat(successCount).isOne();
            assertThat(redisTemplate.opsForValue().get(KEY)).startsWith("new-jti-");
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
