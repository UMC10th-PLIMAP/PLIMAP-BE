package com.example.plimap.domain.place.service.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.place.dto.PlaceAdministrativeRegion;
import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.service.command.PlaceCommandService;
import com.example.plimap.domain.place.service.query.PlaceLocationMetadataService;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
class PlaceAddressSelectionIntegrationTest {

    private static final int REQUEST_COUNT = 8;
    private static final long MEMBER_ID = 930_001L;
    private static final long MISSING_MEMBER_ID = 930_999L;
    private static final String ADDRESS_PREFIX = "주소선택테스트";
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    @Autowired
    private PlaceCommandService placeCommandService;

    @Autowired
    private PlaceQueryService placeQueryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PlaceLocationMetadataService placeLocationMetadataService;

    @MockitoBean
    private PinQueryService pinQueryService;

    @BeforeEach
    void setUp() {
        when(placeLocationMetadataService.getAdministrativeRegion(anyDouble(), anyDouble()))
                .thenReturn(new PlaceAdministrativeRegion(
                        "1168010100",
                        "서울특별시",
                        "강남구",
                        "역삼동"
                ));
        when(pinQueryService.findPinInfosByPlaceIds(
                org.mockito.ArgumentMatchers.<List<Long>>any()
        )).thenReturn(Map.of());
        clearData();
        jdbcTemplate.update("""
                INSERT INTO member (id, nickname, status)
                VALUES (?, '주소선택회원', 'ACTIVE')
                """, MEMBER_ID);
    }

    @AfterEach
    void tearDown() {
        clearData();
    }

    @Test
    void ADDRESS_최초_선택은_행정구역을_포함한_ADDRESS_SEARCH_Place와_이력을_생성한다() {
        PlaceResponse.Selection result = select(
                addressRequest(
                        ADDRESS_PREFIX + " 서울특별시 강남구 역삼동 1",
                        ADDRESS_PREFIX + " 테헤란로 1",
                        37.5000,
                        127.0000,
                        37.5000,
                        127.0000
                )
        );

        Map<String, Object> stored = jdbcTemplate.queryForMap("""
                SELECT name,
                       category,
                       address,
                       road_address,
                       normalized_address,
                       administrative_region_code,
                       sido,
                       sigungu,
                       eup_myeon_dong,
                       place_provider,
                       provider_place_id,
                       source
                FROM place
                WHERE id = ?
                """, result.placeId());

        assertThat(result.placeName()).isEqualTo(ADDRESS_PREFIX + " 테헤란로 1");
        assertThat(result.source().name()).isEqualTo("ADDRESS_SEARCH");
        assertThat(stored)
                .containsEntry("name", ADDRESS_PREFIX + " 테헤란로 1")
                .containsEntry("category", null)
                .containsEntry("address", ADDRESS_PREFIX + " 서울특별시 강남구 역삼동 1")
                .containsEntry("road_address", ADDRESS_PREFIX + " 테헤란로 1")
                .containsEntry(
                        "normalized_address",
                        ADDRESS_PREFIX + "서울특별시강남구역삼동1"
                )
                .containsEntry("administrative_region_code", "1168010100")
                .containsEntry("sido", "서울특별시")
                .containsEntry("sigungu", "강남구")
                .containsEntry("eup_myeon_dong", "역삼동")
                .containsEntry("place_provider", "KAKAO")
                .containsEntry("provider_place_id", null)
                .containsEntry("source", "ADDRESS_SEARCH");
        assertThat(historyCount(MEMBER_ID)).isEqualTo(1L);

        PlaceResponse.SearchHistoryResult histories =
                placeQueryService.getSearchHistories(MEMBER_ID, 37.5000, 127.0000);
        assertThat(histories.items()).singleElement()
                .satisfies(history -> {
                    assertThat(history.placeId()).isEqualTo(result.placeId());
                    assertThat(history.address())
                            .isEqualTo(ADDRESS_PREFIX + " 서울특별시 강남구 역삼동 1");
                    assertThat(history.roadAddress())
                            .isEqualTo(ADDRESS_PREFIX + " 테헤란로 1");
                });
    }

    @Test
    void ADDRESS의_실제_주소_원문은_정규화하지_않고_그대로_저장한다() {
        String address = "  " + ADDRESS_PREFIX + " 서울  강남구 역삼동  원문  ";
        String roadAddress = "  " + ADDRESS_PREFIX + " 테헤란로  원문  ";

        PlaceResponse.Selection result = select(addressRequest(
                address,
                roadAddress,
                37.5000,
                127.0000,
                37.5000,
                127.0000
        ));

        Map<String, Object> stored = jdbcTemplate.queryForMap("""
                SELECT name, address, road_address, normalized_address
                FROM place
                WHERE id = ?
                """, result.placeId());
        assertThat(stored)
                .containsEntry("name", roadAddress)
                .containsEntry("address", address)
                .containsEntry("road_address", roadAddress)
                .containsEntry(
                        "normalized_address",
                        ADDRESS_PREFIX + "서울강남구역삼동원문"
                );
    }

    @Test
    void 같은_정규화_주소는_공백과_요청값이_달라도_기존_Place를_재사용한다() {
        PlaceResponse.Selection first = select(addressRequest(
                ADDRESS_PREFIX + " 서울 강남구 역삼동 2",
                ADDRESS_PREFIX + " 도로 2",
                37.5000,
                127.0000,
                37.5000,
                127.0000
        ));
        PlaceResponse.Selection second = select(addressRequest(
                "  " + ADDRESS_PREFIX + "서울  강남구 역삼동 2 ",
                null,
                37.7000,
                127.2000,
                37.5000,
                127.0000
        ));

        assertThat(second.placeId()).isEqualTo(first.placeId());
        assertThat(second.placeName()).isEqualTo(first.placeName());
        assertThat(second.address()).isEqualTo(first.address());
        assertThat(second.roadAddress()).isEqualTo(first.roadAddress());
        assertThat(addressPlaceCount(
                ADDRESS_PREFIX + "서울강남구역삼동2",
                false
        )).isEqualTo(1L);
        assertThat(historyCount(MEMBER_ID)).isEqualTo(1L);
    }

    @Test
    void 서로_다른_주소는_좌표가_20m_이내여도_별도_Place로_생성한다() {
        PlaceResponse.Selection first = select(addressRequest(
                ADDRESS_PREFIX + " 서울 강남구 역삼동 3",
                null,
                37.5000,
                127.0000,
                37.5000,
                127.0000
        ));
        PlaceResponse.Selection second = select(addressRequest(
                ADDRESS_PREFIX + " 서울 강남구 역삼동 4",
                null,
                37.50001,
                127.00001,
                37.5000,
                127.0000
        ));

        assertThat(second.placeId()).isNotEqualTo(first.placeId());
    }

    @Test
    void Soft_Delete된_동일_주소는_복구하지_않고_새_Place를_생성한다() {
        String address = ADDRESS_PREFIX + " 서울 강남구 역삼동 5";
        PlaceResponse.Selection deleted = select(addressRequest(
                address,
                null,
                37.5000,
                127.0000,
                37.5000,
                127.0000
        ));
        jdbcTemplate.update(
                "UPDATE place SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?",
                deleted.placeId()
        );

        PlaceResponse.Selection created = select(addressRequest(
                address,
                null,
                37.5000,
                127.0000,
                37.5000,
                127.0000
        ));

        assertThat(created.placeId()).isNotEqualTo(deleted.placeId());
        assertThat(addressPlaceCount(
                ADDRESS_PREFIX + "서울강남구역삼동5",
                true
        )).isEqualTo(2L);
        assertThat(addressPlaceCount(
                ADDRESS_PREFIX + "서울강남구역삼동5",
                false
        )).isEqualTo(1L);
    }

    @Test
    void 동일_주소의_동시_요청은_하나의_활성_Place와_같은_placeId를_반환한다()
            throws Exception {
        PlaceRequest.Selection request = addressRequest(
                ADDRESS_PREFIX + " 서울 강남구 역삼동 6",
                null,
                37.5000,
                127.0000,
                37.5000,
                127.0000
        );
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT);

        try {
            List<Future<PlaceResponse.Selection>> futures = new ArrayList<>();
            for (int index = 0; index < REQUEST_COUNT; index++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return select(request);
                }));
            }
            start.countDown();

            List<Long> placeIds = new ArrayList<>();
            for (Future<PlaceResponse.Selection> future : futures) {
                placeIds.add(future.get(30, TimeUnit.SECONDS).placeId());
            }

            assertThat(placeIds).containsOnly(placeIds.getFirst());
            assertThat(addressPlaceCount(
                    ADDRESS_PREFIX + "서울강남구역삼동6",
                    false
            )).isEqualTo(1L);
            assertThat(historyCount(MEMBER_ID)).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void 오백미터_경계는_true이고_초과해도_성공하며_false를_반환한다() {
        PlaceResponse.Selection boundary = select(addressRequest(
                ADDRESS_PREFIX + " 경계 주소",
                null,
                latitudeOffset(500.0),
                0.0,
                0.0,
                0.0
        ));
        PlaceResponse.Selection outside = select(addressRequest(
                ADDRESS_PREFIX + " 초과 주소",
                null,
                latitudeOffset(501.0),
                0.0,
                0.0,
                0.0
        ));

        assertThat(boundary.distanceMeters()).isEqualTo(500);
        assertThat(boundary.withinAccessRange()).isTrue();
        assertThat(outside.distanceMeters()).isEqualTo(501);
        assertThat(outside.withinAccessRange()).isFalse();
    }

    @Test
    void 기존_ADDRESS_SEARCH_Place의_PIN과_북마크_정보를_병합한다() {
        PlaceRequest.Selection request = addressRequest(
                ADDRESS_PREFIX + " PIN 주소",
                null,
                37.5000,
                127.0000,
                37.5000,
                127.0000
        );
        PlaceResponse.Selection created = select(request);
        jdbcTemplate.update(
                "INSERT INTO place_bookmark (place_id, member_id) VALUES (?, ?)",
                created.placeId(),
                MEMBER_ID
        );
        when(pinQueryService.findPinInfosByPlaceIds(List.of(created.placeId())))
                .thenReturn(Map.of(
                        created.placeId(),
                        new PlacePinInfo(true, "핀작성자", 3L)
                ));

        PlaceResponse.Selection selected = select(request);

        assertThat(selected.hasPin()).isTrue();
        assertThat(selected.firstPinCreatorNickname()).isEqualTo("핀작성자");
        assertThat(selected.pinCount()).isEqualTo(3L);
        assertThat(selected.bookmarkedByMe()).isTrue();
    }

    @Test
    void ADDRESS_이력은_재선택_시_갱신되고_회원별_최신_다섯_개만_유지한다() {
        List<Long> placeIds = new ArrayList<>();
        for (int index = 1; index <= 6; index++) {
            placeIds.add(select(addressRequest(
                    ADDRESS_PREFIX + " 이력 주소 " + index,
                    null,
                    37.5000 + index * 0.0001,
                    127.0000,
                    37.5000,
                    127.0000
            )).placeId());
        }
        Long newestHistoryId = jdbcTemplate.queryForObject("""
                SELECT id
                FROM place_search_history
                WHERE member_id = ? AND place_id = ?
                """, Long.class, MEMBER_ID, placeIds.getLast());
        jdbcTemplate.update("""
                UPDATE place_search_history
                SET selected_at = TIMESTAMPTZ '2000-01-01 00:00:00Z'
                WHERE id = ?
                """, newestHistoryId);

        select(addressRequest(
                ADDRESS_PREFIX + " 이력 주소 6",
                null,
                37.5006,
                127.0000,
                37.5000,
                127.0000
        ));

        assertThat(historyCount(MEMBER_ID)).isEqualTo(5L);
        Map<String, Object> refreshed = jdbcTemplate.queryForMap("""
                SELECT id, selected_at
                FROM place_search_history
                WHERE member_id = ? AND place_id = ?
                """, MEMBER_ID, placeIds.getLast());
        assertThat(((Number) refreshed.get("id")).longValue()).isEqualTo(newestHistoryId);
        assertThat(refreshed.get("selected_at").toString()).doesNotStartWith("2000-01-01");
    }

    @Test
    void ADDRESS_요청의_필드_조건을_검증한다() {
        PlaceRequest.Selection providerPlaceIdPresent = new PlaceRequest.Selection(
                "ADDRESS",
                "KAKAO",
                "not-null",
                "무시되는 이름",
                null,
                ADDRESS_PREFIX + " 검증 주소",
                null,
                37.5000,
                127.0000,
                37.5000,
                127.0000
        );
        PlaceRequest.Selection categoryPresent = new PlaceRequest.Selection(
                "ADDRESS",
                "KAKAO",
                null,
                "무시되는 이름",
                "공원",
                ADDRESS_PREFIX + " 검증 주소",
                null,
                37.5000,
                127.0000,
                37.5000,
                127.0000
        );
        PlaceRequest.Selection addressMissing = new PlaceRequest.Selection(
                "ADDRESS",
                "KAKAO",
                null,
                "무시되는 이름",
                null,
                " ",
                null,
                37.5000,
                127.0000,
                37.5000,
                127.0000
        );
        PlaceRequest.Selection blankProviderPlaceId = new PlaceRequest.Selection(
                "ADDRESS",
                "KAKAO",
                " ",
                "무시되는 이름",
                null,
                ADDRESS_PREFIX + " 검증 주소",
                null,
                37.5000,
                127.0000,
                37.5000,
                127.0000
        );
        PlaceRequest.Selection blankCategory = new PlaceRequest.Selection(
                "ADDRESS",
                "KAKAO",
                null,
                "무시되는 이름",
                " ",
                ADDRESS_PREFIX + " 검증 주소",
                null,
                37.5000,
                127.0000,
                37.5000,
                127.0000
        );

        assertThatThrownBy(() -> select(providerPlaceIdPresent))
                .isInstanceOf(PlaceException.class);
        assertThatThrownBy(() -> select(categoryPresent))
                .isInstanceOf(PlaceException.class);
        assertThatThrownBy(() -> select(addressMissing))
                .isInstanceOf(PlaceException.class);
        assertThatThrownBy(() -> select(blankProviderPlaceId))
                .isInstanceOf(PlaceException.class);
        assertThatThrownBy(() -> select(blankCategory))
                .isInstanceOf(PlaceException.class);
        assertThat(addressPlaceTotalCount()).isZero();
        assertThat(historyCount(MEMBER_ID)).isZero();
    }

    @Test
    void 이력_저장에_실패하면_신규_Place도_같이_롤백한다() {
        PlaceRequest.Selection request = addressRequest(
                ADDRESS_PREFIX + " 롤백 주소",
                null,
                37.5000,
                127.0000,
                37.5000,
                127.0000
        );

        assertThatThrownBy(() ->
                placeCommandService.selectSearchPlace(MISSING_MEMBER_ID, request))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(addressPlaceCount(
                ADDRESS_PREFIX + "롤백주소",
                true
        )).isZero();
        assertThat(historyCount(MISSING_MEMBER_ID)).isZero();
    }

    private PlaceResponse.Selection select(PlaceRequest.Selection request) {
        return placeCommandService.selectSearchPlace(MEMBER_ID, request);
    }

    private PlaceRequest.Selection addressRequest(
            String address,
            String roadAddress,
            double latitude,
            double longitude,
            double userLatitude,
            double userLongitude
    ) {
        return new PlaceRequest.Selection(
                "ADDRESS",
                "KAKAO",
                null,
                roadAddress != null ? roadAddress : address,
                null,
                address,
                roadAddress,
                latitude,
                longitude,
                userLatitude,
                userLongitude
        );
    }

    private double latitudeOffset(double distanceMeters) {
        return Math.toDegrees(distanceMeters / EARTH_RADIUS_METERS);
    }

    private long addressPlaceCount(String normalizedAddress, boolean includeDeleted) {
        String deletedCondition = includeDeleted ? "" : " AND deleted_at IS NULL";
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM place"
                        + " WHERE source = 'ADDRESS_SEARCH'"
                        + " AND normalized_address = ?"
                        + deletedCondition,
                Long.class,
                normalizedAddress
        );
    }

    private long addressPlaceTotalCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM place
                WHERE source = 'ADDRESS_SEARCH'
                  AND address LIKE ?
                """, Long.class, ADDRESS_PREFIX + "%");
    }

    private long historyCount(long memberId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM place_search_history
                WHERE member_id = ?
                """, Long.class, memberId);
    }

    private void clearData() {
        jdbcTemplate.update(
                "DELETE FROM place_bookmark WHERE member_id IN (?, ?)",
                MEMBER_ID,
                MISSING_MEMBER_ID
        );
        jdbcTemplate.update(
                "DELETE FROM place_search_history WHERE member_id IN (?, ?)",
                MEMBER_ID,
                MISSING_MEMBER_ID
        );
        jdbcTemplate.update("""
                DELETE FROM place
                WHERE source = 'ADDRESS_SEARCH'
                  AND address LIKE ?
                """, ADDRESS_PREFIX + "%");
        jdbcTemplate.update(
                "DELETE FROM member WHERE id IN (?, ?)",
                MEMBER_ID,
                MISSING_MEMBER_ID
        );
    }
}
