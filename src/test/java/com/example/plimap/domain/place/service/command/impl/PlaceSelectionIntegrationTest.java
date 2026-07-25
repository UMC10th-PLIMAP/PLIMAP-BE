package com.example.plimap.domain.place.service.command.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.support.PostgisContainerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
class PlaceSelectionIntegrationTest {

    private static final int REQUEST_COUNT = 8;

    @Autowired
    private PlaceCommandServiceImpl placeCommandService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        clearPlaces();
    }

    @AfterEach
    void tearDown() {
        clearPlaces();
    }

    @Test
    void 검색_장소를_저장하고_동일한_provider_장소를_멱등하게_재사용한다() {
        PlaceRequest.Selection request = request("26338954");

        PlaceResponse.Selection first = placeCommandService.selectSearchPlace(1L, request);
        PlaceResponse.Selection second = placeCommandService.selectSearchPlace(1L, request);

        Map<String, Object> storedPlace = jdbcTemplate.queryForMap("""
                SELECT name,
                       category,
                       address,
                       road_address,
                       place_provider,
                       provider_place_id,
                       source,
                       ST_X(location::geometry) AS longitude,
                       ST_Y(location::geometry) AS latitude
                FROM place
                WHERE id = ?
                """, first.placeId());
        Long placeCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM place
                WHERE place_provider = 'KAKAO'
                  AND provider_place_id = '26338954'
                """, Long.class);
        Long historyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM place_search_history",
                Long.class
        );

        assertThat(second.placeId()).isEqualTo(first.placeId());
        assertThat(first.source().name()).isEqualTo("PLACE_SEARCH");
        assertThat(first.address()).isEqualTo("서울특별시 영등포구 여의도동");
        assertThat(first.roadAddress()).isEqualTo("서울특별시 영등포구 여의동로");
        assertThat(first.hasPin()).isFalse();
        assertThat(first.firstPinCreatorNickname()).isNull();
        assertThat(first.pinCount()).isZero();
        assertThat(first.bookmarkedByMe()).isFalse();
        assertThat(storedPlace)
                .containsEntry("name", "한강")
                .containsEntry("category", "공원")
                .containsEntry("address", "서울특별시 영등포구 여의도동")
                .containsEntry("road_address", "서울특별시 영등포구 여의동로")
                .containsEntry("place_provider", "KAKAO")
                .containsEntry("provider_place_id", "26338954")
                .containsEntry("source", "PLACE_SEARCH");
        assertThat(((Number) storedPlace.get("longitude")).doubleValue()).isEqualTo(126.9326);
        assertThat(((Number) storedPlace.get("latitude")).doubleValue()).isEqualTo(37.5283);
        assertThat(placeCount).isEqualTo(1L);
        assertThat(historyCount).isZero();
    }

    @Test
    void Soft_Delete된_동일_provider_장소는_복구하지_않고_새로_생성한다() {
        PlaceRequest.Selection request = request("soft-deleted-place");
        PlaceResponse.Selection deleted = placeCommandService.selectSearchPlace(1L, request);
        jdbcTemplate.update(
                "UPDATE place SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?",
                deleted.placeId()
        );

        PlaceResponse.Selection created = placeCommandService.selectSearchPlace(1L, request);

        Long totalCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM place
                WHERE place_provider = 'KAKAO'
                  AND provider_place_id = 'soft-deleted-place'
                """, Long.class);
        Long activeCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM place
                WHERE place_provider = 'KAKAO'
                  AND provider_place_id = 'soft-deleted-place'
                  AND deleted_at IS NULL
                """, Long.class);
        Object deletedAt = jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM place WHERE id = ?",
                Object.class,
                deleted.placeId()
        );

        assertThat(created.placeId()).isNotEqualTo(deleted.placeId());
        assertThat(totalCount).isEqualTo(2L);
        assertThat(activeCount).isEqualTo(1L);
        assertThat(deletedAt).isNotNull();
    }

    @Test
    void 동일_검색_장소의_동시_요청은_같은_placeId를_반환한다() throws Exception {
        PlaceRequest.Selection request = request("concurrent-place");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT);

        try {
            List<Future<PlaceResponse.Selection>> futures = new ArrayList<>();
            for (int index = 0; index < REQUEST_COUNT; index++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return placeCommandService.selectSearchPlace(1L, request);
                }));
            }

            start.countDown();

            List<Long> placeIds = new ArrayList<>();
            for (Future<PlaceResponse.Selection> future : futures) {
                placeIds.add(future.get(30, TimeUnit.SECONDS).placeId());
            }

            Long activeCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM place
                    WHERE place_provider = 'KAKAO'
                      AND provider_place_id = 'concurrent-place'
                      AND deleted_at IS NULL
                    """, Long.class);

            assertThat(placeIds).containsOnly(placeIds.getFirst());
            assertThat(activeCount).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    private PlaceRequest.Selection request(String providerPlaceId) {
        return new PlaceRequest.Selection(
                "KAKAO",
                providerPlaceId,
                "한강",
                "공원",
                "서울특별시 영등포구 여의도동",
                "서울특별시 영등포구 여의동로",
                37.5283,
                126.9326,
                37.5251,
                126.9298
        );
    }

    private void clearPlaces() {
        jdbcTemplate.update("DELETE FROM place_search_history");
        jdbcTemplate.update("DELETE FROM place_bookmark");
        jdbcTemplate.update("DELETE FROM place");
    }
}
