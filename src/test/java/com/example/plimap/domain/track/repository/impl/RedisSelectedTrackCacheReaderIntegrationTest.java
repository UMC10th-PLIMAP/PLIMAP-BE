package com.example.plimap.domain.track.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.plimap.domain.track.dto.SelectedTrackCache;
import com.example.plimap.domain.track.repository.SelectedTrackCacheRepository;
import com.example.plimap.domain.track.service.query.SelectedTrackCacheReader;
import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
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
class RedisSelectedTrackCacheReaderIntegrationTest {

    private static final Long ITUNES_TRACK_ID = 123456789L;
    private static final String KEY = "track:selection:" + ITUNES_TRACK_ID;

    @Autowired
    private SelectedTrackCacheRepository selectedTrackCacheRepository;

    @Autowired
    private SelectedTrackCacheReader selectedTrackCacheReader;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    @AfterEach
    void clearSelectionCache() {
        redisTemplate.delete(KEY);
    }

    @Test
    void selection_Repository가_Reader_Bean으로_등록된다() {
        assertThat(selectedTrackCacheReader).isSameAs(selectedTrackCacheRepository);
        assertThat(selectedTrackCacheReader)
                .isInstanceOf(RedisSelectedTrackCacheRepository.class);
    }

    @Test
    void Writer가_저장한_selection을_Reader로_조회한다() {
        SelectedTrackCache selectedTrack = selectedTrack();
        selectedTrackCacheRepository.save(selectedTrack);

        assertThat(selectedTrackCacheReader.findByItunesTrackId(ITUNES_TRACK_ID))
                .contains(selectedTrack);
        assertThat(redisTemplate.hasKey(KEY)).isTrue();
    }

    private SelectedTrackCache selectedTrack() {
        return new SelectedTrackCache(
                ITUNES_TRACK_ID,
                "abcdefghijk",
                "밤편지",
                "아이유",
                "Palette",
                "https://image.example/cover.jpg",
                "https://audio.example/preview.m4a",
                253000
        );
    }
}
