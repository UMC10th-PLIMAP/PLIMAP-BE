package com.example.plimap.global.config;

import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import({PostgisContainerConfiguration.class, RedisContainerConfiguration.class})
class RedisConnectivityTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void Redis에_값을_저장하고_TTL과_함께_조회할_수_있다() {
        redisTemplate.opsForValue().set("connectivity-check", "ok", Duration.ofSeconds(30));

        assertThat(redisTemplate.opsForValue().get("connectivity-check")).isEqualTo("ok");
        assertThat(redisTemplate.getExpire("connectivity-check")).isGreaterThan(0);
    }
}
