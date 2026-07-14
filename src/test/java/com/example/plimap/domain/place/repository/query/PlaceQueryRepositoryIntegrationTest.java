package com.example.plimap.domain.place.repository.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.support.PostgisContainerConfiguration;
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
class PlaceQueryRepositoryIntegrationTest {

    private static final double LATITUDE = 37.5283;
    private static final double LONGITUDE = 126.9326;

    @Autowired
    private PlaceQueryRepository placeQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 장소_거리와_조회_반경이_같으면_MAP_SELECTION_장소를_조회한다() {
        Long placeId = insertMapSelection(20.0, 90.0, false);
        Double actualDistance = jdbcTemplate.queryForObject("""
                SELECT ST_Distance(
                    location,
                    ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography
                )
                FROM place
                WHERE id = ?
                """, Double.class, LONGITUDE, LATITUDE, placeId);

        Place result = placeQueryRepository
                .findNearestActiveMapSelectionWithin(LATITUDE, LONGITUDE, actualDistance)
                .orElseThrow();

        assertThat(actualDistance).isCloseTo(20.0, within(0.001));
        assertThat(result.getId()).isEqualTo(placeId);
    }

    @Test
    void 가장_가깝고_id가_작은_장소를_우선한다() {
        insertMapSelection(15.0, 0.0, false);
        Long firstAtTenMeters = insertMapSelection(10.0, 90.0, false);
        insertMapSelection(10.0, 270.0, false);

        Place result = placeQueryRepository
                .findNearestActiveMapSelectionWithin(LATITUDE, LONGITUDE, 20.0)
                .orElseThrow();

        assertThat(result.getId()).isEqualTo(firstAtTenMeters);
    }

    @Test
    void 삭제된_장소와_provider_장소와_20m_초과_장소를_제외한다() {
        insertMapSelection(5.0, 0.0, true);
        insertSearchPlace(3.0);
        insertMapSelection(21.0, 90.0, false);
        Long expectedPlaceId = insertMapSelection(12.0, 180.0, false);

        Place result = placeQueryRepository
                .findNearestActiveMapSelectionWithin(LATITUDE, LONGITUDE, 20.0)
                .orElseThrow();

        assertThat(result.getId()).isEqualTo(expectedPlaceId);
        assertThat(result.getPlaceProvider()).isNull();
        assertThat(result.getProviderPlaceId()).isNull();
    }

    private Long insertMapSelection(double distanceMeters, double bearingDegrees, boolean deleted) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO place (
                    name,
                    address,
                    source,
                    location,
                    deleted_at
                )
                VALUES (
                    '지도 선택 장소',
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
                """, Long.class, LONGITUDE, LATITUDE, distanceMeters, bearingDegrees, deleted);
    }

    private Long insertSearchPlace(double distanceMeters) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO place (
                    name,
                    address,
                    place_provider,
                    provider_place_id,
                    source,
                    location
                )
                VALUES (
                    '검색 장소',
                    '지번 주소',
                    'KAKAO',
                    'provider-place-id',
                    'PLACE_SEARCH',
                    ST_Project(
                        ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography,
                        ?,
                        radians(0)
                    )
                )
                RETURNING id
                """, Long.class, LONGITUDE, LATITUDE, distanceMeters);
    }
}
