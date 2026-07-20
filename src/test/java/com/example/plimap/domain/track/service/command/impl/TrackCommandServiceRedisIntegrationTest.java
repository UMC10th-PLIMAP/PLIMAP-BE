package com.example.plimap.domain.track.service.command.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.track.dto.SelectedTrackCache;
import com.example.plimap.domain.track.dto.request.TrackCommand;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.repository.SelectedTrackCacheRepository;
import com.example.plimap.domain.track.service.command.TrackCommandService;
import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import({PostgisContainerConfiguration.class, RedisContainerConfiguration.class})
@Transactional
class TrackCommandServiceRedisIntegrationTest {

    private static final Long ITUNES_TRACK_ID = 987654321L;
    private static final String KEY = "track:selection:" + ITUNES_TRACK_ID;

    @Autowired
    private TrackCommandService trackCommandService;

    @Autowired
    private SelectedTrackCacheRepository selectedTrackCacheRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void clearSelectionCache() {
        redisTemplate.delete(KEY);
    }

    @Test
    void Redis_Reader_Bean으로_selection을_읽어_Track과_PlaceTrack을_생성한다() {
        Place place = savePlace();
        selectedTrackCacheRepository.save(selectedTrack());

        PlaceTrack result = trackCommandService.getOrCreatePlaceTrack(
                place,
                new TrackCommand.Create(ITUNES_TRACK_ID)
        );

        assertThat(result.getTrack().getProvider()).isEqualTo("YOUTUBE");
        assertThat(result.getTrack().getProviderTrackId()).isEqualTo("abcdefghijk");
        assertThat(result.getTrack().getTitle()).isEqualTo("밤편지");
        assertThat(result.getTrack().getArtistName()).isEqualTo("아이유");
        assertThat(result.getTrack().getAlbumImageUrl())
                .isEqualTo("https://image.example/cover.jpg");
        assertThat(result.getPlace().getId()).isEqualTo(place.getId());
    }

    private Place savePlace() {
        Point location = new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(127.0, 37.0));
        Place place = Place.builder()
                .name("테스트 장소")
                .address("서울특별시 테스트구")
                .source(PlaceSource.MAP_SELECTION)
                .location(location)
                .build();
        entityManager.persist(place);
        entityManager.flush();
        return place;
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
