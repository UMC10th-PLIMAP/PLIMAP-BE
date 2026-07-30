package com.example.plimap.domain.place.service.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.place.dto.PlaceAdministrativeRegion;
import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.service.query.PlaceLocationMetadataService;
import com.example.plimap.domain.place.service.command.PlaceCommandService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
class PlaceSelectionIntegrationTest {

    private static final int REQUEST_COUNT = 8;
    private static final long MEMBER_ID = 900_001L;
    private static final long OTHER_MEMBER_ID = 900_002L;
    private static final String TEST_PROVIDER_PLACE_ID_PREFIX = "place-selection-test-";

    @Autowired
    private PlaceCommandService placeCommandService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PlaceLocationMetadataService placeLocationMetadataService;

    @BeforeEach
    void setUp() {
        when(placeLocationMetadataService.getAdministrativeRegion(anyDouble(), anyDouble()))
                .thenReturn(new PlaceAdministrativeRegion(
                        "1156054000",
                        "서울특별시",
                        "영등포구",
                        "여의동"
                ));
        clearPlaces();
        clearMembers();
        insertMembers();
    }

    @AfterEach
    void tearDown() {
        clearPlaces();
        clearMembers();
    }

    @Test
    void 검색_장소를_저장하고_동일한_provider_장소를_멱등하게_재사용한다() {
        PlaceRequest.Selection request = request("26338954");

        PlaceResponse.Selection first = placeCommandService.selectSearchPlace(MEMBER_ID, request);
        PlaceResponse.Selection second = placeCommandService.selectSearchPlace(MEMBER_ID, request);

        Map<String, Object> storedPlace = jdbcTemplate.queryForMap("""
                SELECT name,
                       category,
                       address,
                       road_address,
                       administrative_region_code,
                       sido,
                       sigungu,
                       eup_myeon_dong,
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
                  AND provider_place_id = ?
                """, Long.class, testProviderPlaceId("26338954"));
        Long historyCount = historyCount(MEMBER_ID);
        Map<String, Object> storedHistory = jdbcTemplate.queryForMap("""
                SELECT member_id, place_id
                FROM place_search_history
                WHERE member_id = ?
                """, MEMBER_ID);

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
                .containsEntry("administrative_region_code", "1156054000")
                .containsEntry("sido", "서울특별시")
                .containsEntry("sigungu", "영등포구")
                .containsEntry("eup_myeon_dong", "여의동")
                .containsEntry("place_provider", "KAKAO")
                .containsEntry("provider_place_id", testProviderPlaceId("26338954"))
                .containsEntry("source", "PLACE_SEARCH");
        assertThat(((Number) storedPlace.get("longitude")).doubleValue()).isEqualTo(126.9326);
        assertThat(((Number) storedPlace.get("latitude")).doubleValue()).isEqualTo(37.5283);
        assertThat(placeCount).isEqualTo(1L);
        assertThat(historyCount).isEqualTo(1L);
        assertThat(((Number) storedHistory.get("member_id")).longValue())
                .isEqualTo(MEMBER_ID);
        assertThat(((Number) storedHistory.get("place_id")).longValue())
                .isEqualTo(first.placeId());
    }

    @Test
    void Soft_Delete된_동일_provider_장소는_복구하지_않고_새로_생성한다() {
        PlaceRequest.Selection request = request("soft-deleted-place");
        PlaceResponse.Selection deleted =
                placeCommandService.selectSearchPlace(MEMBER_ID, request);
        jdbcTemplate.update(
                "UPDATE place SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?",
                deleted.placeId()
        );

        PlaceResponse.Selection created =
                placeCommandService.selectSearchPlace(MEMBER_ID, request);

        Long totalCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM place
                WHERE place_provider = 'KAKAO'
                  AND provider_place_id = ?
                """, Long.class, testProviderPlaceId("soft-deleted-place"));
        Long activeCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM place
                WHERE place_provider = 'KAKAO'
                  AND provider_place_id = ?
                  AND deleted_at IS NULL
                """, Long.class, testProviderPlaceId("soft-deleted-place"));
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
                    return placeCommandService.selectSearchPlace(MEMBER_ID, request);
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
                      AND provider_place_id = ?
                      AND deleted_at IS NULL
                    """, Long.class, testProviderPlaceId("concurrent-place"));

            assertThat(placeIds).containsOnly(placeIds.getFirst());
            assertThat(activeCount).isEqualTo(1L);
            assertThat(historyCount(MEMBER_ID)).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void 동일_장소를_재선택하면_행을_추가하지_않고_selectedAt을_갱신한다() {
        PlaceRequest.Selection request = request("reselected-place");
        placeCommandService.selectSearchPlace(MEMBER_ID, request);
        Long historyId = jdbcTemplate.queryForObject(
                "SELECT id FROM place_search_history WHERE member_id = ?",
                Long.class,
                MEMBER_ID
        );
        jdbcTemplate.update(
                "UPDATE place_search_history SET selected_at = TIMESTAMPTZ '2000-01-01 00:00:00Z'"
                        + " WHERE id = ?",
                historyId
        );

        placeCommandService.selectSearchPlace(MEMBER_ID, request);

        Map<String, Object> history = jdbcTemplate.queryForMap("""
                SELECT id, selected_at
                FROM place_search_history
                WHERE member_id = ?
                """, MEMBER_ID);
        assertThat(((Number) history.get("id")).longValue()).isEqualTo(historyId);
        assertThat(history.get("selected_at").toString()).doesNotStartWith("2000-01-01");
        assertThat(historyCount(MEMBER_ID)).isEqualTo(1L);
    }

    @Test
    void 서로_다른_여섯_장소를_선택하면_최신_다섯_개만_유지한다() {
        for (int index = 1; index <= 6; index++) {
            placeCommandService.selectSearchPlace(MEMBER_ID, request("place-" + index));
        }

        List<String> providerPlaceIds = jdbcTemplate.queryForList("""
                SELECT place.provider_place_id
                FROM place_search_history history
                JOIN place ON place.id = history.place_id
                WHERE history.member_id = ?
                ORDER BY history.selected_at DESC, history.id DESC
                """, String.class, MEMBER_ID);

        assertThat(providerPlaceIds)
                .hasSize(5)
                .containsExactly(
                        testProviderPlaceId("place-6"),
                        testProviderPlaceId("place-5"),
                        testProviderPlaceId("place-4"),
                        testProviderPlaceId("place-3"),
                        testProviderPlaceId("place-2")
                );
    }

    @Test
    void 최근_검색_이력은_회원별로_분리한다() {
        PlaceRequest.Selection request = request("shared-place");

        PlaceResponse.Selection first =
                placeCommandService.selectSearchPlace(MEMBER_ID, request);
        PlaceResponse.Selection second =
                placeCommandService.selectSearchPlace(OTHER_MEMBER_ID, request);

        assertThat(second.placeId()).isEqualTo(first.placeId());
        assertThat(historyCount(MEMBER_ID)).isEqualTo(1L);
        assertThat(historyCount(OTHER_MEMBER_ID)).isEqualTo(1L);
    }

    @Test
    void 장소_선택에_실패하면_최근_검색_이력을_저장하지_않는다() {
        PlaceRequest.Selection invalid = new PlaceRequest.Selection(
                "NAVER",
                "invalid-place",
                "invalid",
                null,
                "address",
                null,
                37.0,
                127.0,
                37.0,
                127.0
        );

        assertThatThrownBy(() -> placeCommandService.selectSearchPlace(MEMBER_ID, invalid))
                .isInstanceOf(PlaceException.class);

        assertThat(historyCount(MEMBER_ID)).isZero();
    }

    @Test
    void 서로_다른_장소를_동시에_선택해도_중복_없이_최대_다섯_개만_유지한다() throws Exception {
        int selectionCount = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(selectionCount);

        try {
            List<Future<PlaceResponse.Selection>> futures = new ArrayList<>();
            for (int index = 0; index < selectionCount; index++) {
                String providerPlaceId = "concurrent-history-" + index;
                futures.add(executor.submit(() -> {
                    start.await();
                    return placeCommandService.selectSearchPlace(
                            MEMBER_ID,
                            request(providerPlaceId)
                    );
                }));
            }

            start.countDown();
            for (Future<PlaceResponse.Selection> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }

            Long totalCount = historyCount(MEMBER_ID);
            Long distinctPlaceCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(DISTINCT place_id)
                    FROM place_search_history
                    WHERE member_id = ?
                    """, Long.class, MEMBER_ID);

            assertThat(totalCount).isEqualTo(5L);
            assertThat(distinctPlaceCount).isEqualTo(5L);
        } finally {
            executor.shutdownNow();
        }
    }

    private PlaceRequest.Selection request(String providerPlaceId) {
        return new PlaceRequest.Selection(
                "KAKAO",
                testProviderPlaceId(providerPlaceId),
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
        jdbcTemplate.update(
                "DELETE FROM place_search_history WHERE member_id IN (?, ?)",
                MEMBER_ID,
                OTHER_MEMBER_ID
        );
        jdbcTemplate.update(
                "DELETE FROM place_bookmark WHERE member_id IN (?, ?)",
                MEMBER_ID,
                OTHER_MEMBER_ID
        );
        jdbcTemplate.update("""
                DELETE FROM place
                WHERE place_provider = 'KAKAO'
                  AND provider_place_id LIKE ?
                """, TEST_PROVIDER_PLACE_ID_PREFIX + "%");
    }

    private void insertMembers() {
        jdbcTemplate.update("""
                INSERT INTO member (id, nickname, status)
                VALUES (?, 'history1', 'ACTIVE'), (?, 'history2', 'ACTIVE')
                """, MEMBER_ID, OTHER_MEMBER_ID);
    }

    private void clearMembers() {
        jdbcTemplate.update(
                "DELETE FROM member WHERE id IN (?, ?)",
                MEMBER_ID,
                OTHER_MEMBER_ID
        );
    }

    private Long historyCount(long memberId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM place_search_history
                WHERE member_id = ?
                """, Long.class, memberId);
    }

    private String testProviderPlaceId(String providerPlaceId) {
        return TEST_PROVIDER_PLACE_ID_PREFIX + providerPlaceId;
    }
}
