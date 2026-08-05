package com.example.plimap.domain.place.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.plimap.domain.pin.repository.PinRepository;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.enums.PopularPlaceScope;
import com.example.plimap.domain.place.repository.PlaceBookmarkRepository;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.track.repository.PlaceTrackRepository;
import com.example.plimap.domain.track.repository.TrackRepository;
import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import({PostgisContainerConfiguration.class, RedisContainerConfiguration.class})
@Transactional
class PopularPlaceListIntegrationTest {

    private static final double LATITUDE = 37.5283;
    private static final double LONGITUDE = 126.9326;

    @Autowired
    private PlaceQueryService placeQueryService;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private PlaceBookmarkRepository placeBookmarkRepository;

    @Autowired
    private PinRepository pinRepository;

    @Autowired
    private PlaceTrackRepository placeTrackRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private PinQueryService pinQueryService;

    @Test
    void 대표_이미지를_한_번의_배치_조회로_조립하고_조회_전후_데이터를_변경하지_않는다() {
        Long placeId = insertPlace("인기 장소", 100.0);
        Long placeTrackId = insertPlaceTrack(placeId, "https://image/representative");
        insertPin(placeId, placeTrackId, false);
        insertPin(placeId, placeTrackId, true);
        long placeCount = placeRepository.count();
        long bookmarkCount = placeBookmarkRepository.count();
        long pinCount = pinRepository.count();
        long placeTrackCount = placeTrackRepository.count();
        long trackCount = trackRepository.count();

        PlaceResponse.PopularListResult result = placeQueryService.getPopularPlaces(
                PopularPlaceScope.NEARBY,
                LATITUDE,
                LONGITUDE
        );

        assertThat(result.items()).containsExactly(
                new PlaceResponse.PopularListItem(
                        placeId,
                        "인기 장소",
                        100,
                        2L,
                        "https://image/representative"
                )
        );
        verify(pinQueryService, times(1))
                .findRepresentativePlaceTracksByPlaceIds(List.of(placeId));
        assertThat(placeRepository.count()).isEqualTo(placeCount);
        assertThat(placeBookmarkRepository.count()).isEqualTo(bookmarkCount);
        assertThat(pinRepository.count()).isEqualTo(pinCount);
        assertThat(placeTrackRepository.count()).isEqualTo(placeTrackCount);
        assertThat(trackRepository.count()).isEqualTo(trackCount);
    }

    private Long insertPlace(String name, double distanceMeters) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO place (name, address, source, location)
                VALUES (
                    ?,
                    '지번 주소',
                    'MAP_SELECTION',
                    ST_Project(
                        ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography,
                        ?,
                        radians(0)
                    )
                )
                RETURNING id
                """, Long.class, name, LONGITUDE, LATITUDE, distanceMeters);
    }

    private Long insertPlaceTrack(Long placeId, String albumImageUrl) {
        Long trackId = jdbcTemplate.queryForObject("""
                INSERT INTO track (
                    provider,
                    provider_track_id,
                    title,
                    artist_name,
                    album_image_url
                )
                VALUES ('TEST', ?, '테스트 곡', '테스트 가수', ?)
                RETURNING id
                """, Long.class, "popular-service-track-" + placeId, albumImageUrl);
        return jdbcTemplate.queryForObject("""
                INSERT INTO place_track (place_id, track_id)
                VALUES (?, ?)
                RETURNING id
                """, Long.class, placeId, trackId);
    }

    private void insertPin(Long placeId, Long placeTrackId, boolean feedPublic) {
        Long memberId = jdbcTemplate.queryForObject(
                "INSERT INTO member DEFAULT VALUES RETURNING id",
                Long.class
        );
        jdbcTemplate.update("""
                INSERT INTO pin (
                    member_id,
                    place_id,
                    place_track_id,
                    clip_start_ms,
                    introduction,
                    is_feed_public
                )
                VALUES (?, ?, ?, 0, '', ?)
                """, memberId, placeId, placeTrackId, feedPublic);
    }
}
