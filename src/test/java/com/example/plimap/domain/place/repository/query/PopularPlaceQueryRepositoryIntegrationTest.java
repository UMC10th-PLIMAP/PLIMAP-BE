package com.example.plimap.domain.place.repository.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.example.plimap.domain.place.dto.PopularPlaceCandidate;
import com.example.plimap.support.PostgisContainerConfiguration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
@Transactional
class PopularPlaceQueryRepositoryIntegrationTest {

    private static final double LATITUDE = 37.5283;
    private static final double LONGITUDE = 126.9326;

    @Autowired
    private PopularPlaceQueryRepository popularPlaceQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void NEARBY는_정확히_500m를_포함하고_초과_장소와_비활성_데이터를_제외한다() {
        Long boundaryId = insertPlace("경계 장소", 500.0, 0.0, false);
        Long outsideId = insertPlace("경계 밖", 500.01, 0.0, false);
        Long noActivePinId = insertPlace("활성 PIN 없음", 100.0, 0.0, false);
        Long deletedPlaceId = insertPlace("삭제 장소", 50.0, 0.0, true);
        insertPins(boundaryId, 1, false, false);
        insertPins(outsideId, 1, true, false);
        insertPins(noActivePinId, 1, true, true);
        insertPins(deletedPlaceId, 1, true, false);

        List<PopularPlaceCandidate> result =
                popularPlaceQueryRepository.findNearbyPopularPlaces(
                        LATITUDE,
                        LONGITUDE
                );

        assertThat(distanceFrom(boundaryId)).isCloseTo(500.0, within(0.001));
        assertThat(distanceFrom(outsideId)).isGreaterThan(500.0);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().placeId()).isEqualTo(boundaryId);
        assertThat(result.getFirst().pinCount()).isEqualTo(1L);
        assertThat(result.getFirst().distanceMeters()).isCloseTo(500.0, within(0.001));
    }

    @Test
    void NEARBY는_반올림_전_실제거리_PIN수_placeId순으로_정렬한다() {
        Long roundedFartherId = insertPlace("100.4m", 100.4, 0.0, false);
        Long roundedNearerId = insertPlace("100.1m", 100.1, 0.0, false);
        Long morePinsId = insertPlace("동거리 PIN 2개", 200.0, 90.0, false);
        Long fewerPinsId = insertPlace("동거리 PIN 1개", 200.0, 90.0, false);
        Long firstId = insertPlace("ID 우선 1", 300.0, 180.0, false);
        Long secondId = insertPlace("ID 우선 2", 300.0, 180.0, false);
        insertPins(roundedFartherId, 1, true, false);
        insertPins(roundedNearerId, 1, true, false);
        insertPins(morePinsId, 2, true, false);
        insertPins(fewerPinsId, 1, true, false);
        insertPins(firstId, 1, true, false);
        insertPins(secondId, 1, true, false);

        List<PopularPlaceCandidate> result =
                popularPlaceQueryRepository.findNearbyPopularPlaces(
                        LATITUDE,
                        LONGITUDE
                );

        assertThat(result).extracting(PopularPlaceCandidate::placeId)
                .containsExactly(
                        roundedNearerId,
                        roundedFartherId,
                        morePinsId,
                        fewerPinsId,
                        firstId,
                        secondId
                );
    }

    @Test
    void GLOBAL은_PIN수_실제거리_placeId순으로_정렬한다() {
        Long mostPinsId = insertPlace("PIN 3개", 400.0, 0.0, false);
        Long tiedNearId = insertPlace("PIN 2개 가까움", 100.0, 0.0, false);
        Long tiedFarId = insertPlace("PIN 2개 멂", 300.0, 0.0, false);
        Long firstId = insertPlace("ID 우선 1", 500.0, 90.0, false);
        Long secondId = insertPlace("ID 우선 2", 500.0, 90.0, false);
        insertPins(mostPinsId, 3, false, false);
        insertPins(tiedNearId, 2, false, false);
        insertPins(tiedFarId, 2, true, false);
        insertPins(firstId, 1, true, false);
        insertPins(secondId, 1, false, false);

        List<PopularPlaceCandidate> result =
                popularPlaceQueryRepository.findGlobalPopularPlaces(
                        LATITUDE,
                        LONGITUDE
                );

        assertThat(result).extracting(PopularPlaceCandidate::placeId)
                .containsExactly(mostPinsId, tiedNearId, tiedFarId, firstId, secondId);
        assertThat(result).extracting(PopularPlaceCandidate::pinCount)
                .containsExactly(3L, 2L, 2L, 1L, 1L);
    }

    @Test
    void 각_scope는_DB에서_정렬한_뒤_최대_6개만_반환한다() {
        List<Long> idsByDistance = new ArrayList<>();
        for (int index = 1; index <= 7; index++) {
            Long placeId = insertPlace("장소 " + index, index * 10.0, 0.0, false);
            idsByDistance.add(placeId);
            insertPins(placeId, 1, true, false);
        }

        List<PopularPlaceCandidate> nearby =
                popularPlaceQueryRepository.findNearbyPopularPlaces(
                        LATITUDE,
                        LONGITUDE
                );
        List<PopularPlaceCandidate> global =
                popularPlaceQueryRepository.findGlobalPopularPlaces(
                        LATITUDE,
                        LONGITUDE
                );

        assertThat(nearby).hasSize(6);
        assertThat(global).hasSize(6);
        assertThat(nearby).extracting(PopularPlaceCandidate::placeId)
                .containsExactlyElementsOf(idsByDistance.subList(0, 6));
        assertThat(global).extracting(PopularPlaceCandidate::placeId)
                .containsExactlyElementsOf(idsByDistance.subList(0, 6));
    }

    private Long insertPlace(
            String name,
            double distanceMeters,
            double bearingDegrees,
            boolean deleted
    ) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO place (name, address, source, location, deleted_at)
                VALUES (
                    ?,
                    '지번 주소',
                    'MAP_SELECTION',
                    ST_Project(
                        ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography,
                        ?,
                        radians(?)
                    ),
                    CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END
                )
                RETURNING id
                """, Long.class,
                name,
                LONGITUDE,
                LATITUDE,
                distanceMeters,
                bearingDegrees,
                deleted);
    }

    private void insertPins(
            Long placeId,
            int count,
            boolean feedPublic,
            boolean deleted
    ) {
        Long trackId = jdbcTemplate.queryForObject("""
                INSERT INTO track (provider, provider_track_id, title, artist_name)
                VALUES ('TEST', ?, '테스트 곡', '테스트 가수')
                RETURNING id
                """, Long.class, "popular-track-" + placeId);
        Long placeTrackId = jdbcTemplate.queryForObject("""
                INSERT INTO place_track (place_id, track_id)
                VALUES (?, ?)
                RETURNING id
                """, Long.class, placeId, trackId);

        for (int index = 0; index < count; index++) {
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
                        is_feed_public,
                        deleted_at
                    )
                    VALUES (
                        ?, ?, ?, 0, '', ?,
                        CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END
                    )
                    """, memberId, placeId, placeTrackId, feedPublic, deleted);
        }
    }

    private Double distanceFrom(Long placeId) {
        return jdbcTemplate.queryForObject("""
                SELECT ST_Distance(
                    location,
                    ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography
                )
                FROM place
                WHERE id = ?
                """, Double.class, LONGITUDE, LATITUDE, placeId);
    }
}
