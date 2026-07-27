package com.example.plimap.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.service.command.PlaceCommandService;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
import com.example.plimap.support.PostgisContainerConfiguration;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
class PlaceSearchHistoryIntegrationTest {

    private static final long MEMBER_ID = 910_001L;
    private static final long OTHER_MEMBER_ID = 910_002L;
    private static final String PROVIDER_PLACE_ID_PREFIX = "history-api-test-";

    @Autowired
    private PlaceQueryService placeQueryService;

    @Autowired
    private PlaceCommandService placeCommandService;

    @MockitoSpyBean
    private PinQueryService pinQueryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        clearData();
        jdbcTemplate.update("""
                INSERT INTO member (id, nickname, status)
                VALUES (?, '이력회원', 'ACTIVE'), (?, '다른회원', 'ACTIVE')
                """, MEMBER_ID, OTHER_MEMBER_ID);
    }

    @AfterEach
    void tearDown() {
        clearData();
    }

    @Test
    void 최신_다섯_개를_먼저_정렬한_후_Soft_Delete된_Place_이력만_제외한다() {
        List<HistoryFixture> fixtures = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            int selectedDay = index == 2 ? 23 : 20 + index;
            Instant selectedAt = Instant.parse(
                    "2026-07-%02dT00:00:00Z".formatted(selectedDay)
            );
            fixtures.add(insertHistory(MEMBER_ID, "ordered-" + index, selectedAt, index));
        }
        HistoryFixture deletedWithinTopFive = fixtures.get(4);
        jdbcTemplate.update(
                "UPDATE place SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?",
                deletedWithinTopFive.placeId()
        );

        List<Instant> selectedAtBefore = selectedTimes(MEMBER_ID);

        PlaceResponse.SearchHistoryResult result =
                placeQueryService.getSearchHistories(MEMBER_ID, 37.5283, 126.9326);

        assertThat(result.items())
                .extracting(PlaceResponse.SearchHistoryItem::historyId)
                .containsExactly(
                        fixtures.get(5).historyId(),
                        fixtures.get(3).historyId(),
                        fixtures.get(2).historyId(),
                        fixtures.get(1).historyId()
                )
                .doesNotContain(fixtures.getFirst().historyId());
        assertThat(result.items()).hasSize(4);
        assertThat(historyCount(MEMBER_ID)).isEqualTo(6L);
        assertThat(selectedTimes(MEMBER_ID)).containsExactlyElementsOf(selectedAtBefore);
    }

    @Test
    void Place가_연결되지_않은_provider_only_이력은_부작용_없이_목록에서_제외한다() {
        HistoryFixture active = insertHistory(
                MEMBER_ID,
                "provider-only-active",
                Instant.parse("2026-07-26T00:00:00Z"),
                0
        );
        String providerPlaceId = PROVIDER_PLACE_ID_PREFIX + "provider-only";
        Long providerOnlyHistoryId = insertProviderOnlyHistory(
                providerPlaceId,
                Instant.parse("2026-07-27T00:00:00Z")
        );
        Map<String, Object> providerOnlyBefore = historySnapshot(providerOnlyHistoryId);

        PlaceResponse.SearchHistoryResult result =
                placeQueryService.getSearchHistories(MEMBER_ID, 37.5283, 126.9326);

        assertThat(result.items())
                .extracting(PlaceResponse.SearchHistoryItem::historyId)
                .containsExactly(active.historyId())
                .doesNotContain(providerOnlyHistoryId);
        verify(pinQueryService).findPinInfosByPlaceIds(List.of(active.placeId()));
        verifyNoMoreInteractions(pinQueryService);
        assertThat(placeCountByProviderPlaceId(providerPlaceId)).isZero();
        assertThat(historyExists(providerOnlyHistoryId)).isTrue();
        assertThat(historyCount(MEMBER_ID)).isEqualTo(2L);
        assertThat(historySnapshot(providerOnlyHistoryId))
                .containsExactlyEntriesOf(providerOnlyBefore);
    }

    @Test
    void 소유한_이력만_물리_삭제하고_Place와_다른_사용자_이력은_유지한다() {
        HistoryFixture owned = insertHistory(
                MEMBER_ID,
                "delete-owned",
                Instant.parse("2026-07-27T00:00:00Z"),
                0
        );
        Long otherHistoryId = insertHistoryForPlace(
                OTHER_MEMBER_ID,
                owned.placeId(),
                Instant.parse("2026-07-27T01:00:00Z")
        );

        placeCommandService.deleteSearchHistory(MEMBER_ID, owned.historyId());

        assertThat(historyExists(owned.historyId())).isFalse();
        assertThat(placeExists(owned.placeId())).isTrue();
        assertThat(historyExists(otherHistoryId)).isTrue();

        assertThatThrownBy(() ->
                placeCommandService.deleteSearchHistory(MEMBER_ID, otherHistoryId))
                .isInstanceOf(PlaceException.class)
                .extracting(exception -> ((PlaceException) exception).getErrorCode())
                .isEqualTo(PlaceErrorCode.PLACE_SEARCH_HISTORY_NOT_FOUND);
        assertThat(historyExists(otherHistoryId)).isTrue();
    }

    private HistoryFixture insertHistory(
            long memberId,
            String suffix,
            Instant selectedAt,
            int coordinateOffset
    ) {
        Long placeId = jdbcTemplate.queryForObject("""
                INSERT INTO place (
                    name,
                    category,
                    address,
                    road_address,
                    place_provider,
                    provider_place_id,
                    source,
                    location
                )
                VALUES (
                    ?,
                    '공원',
                    ?,
                    ?,
                    'KAKAO',
                    ?,
                    'PLACE_SEARCH',
                    ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography
                )
                RETURNING id
                """,
                Long.class,
                "장소-" + suffix,
                "지번-" + suffix,
                coordinateOffset % 2 == 0 ? null : "도로명-" + suffix,
                PROVIDER_PLACE_ID_PREFIX + suffix,
                126.9326 + coordinateOffset * 0.0001,
                37.5283 + coordinateOffset * 0.0001
        );
        Long historyId = insertHistoryForPlace(memberId, placeId, selectedAt);
        return new HistoryFixture(placeId, historyId);
    }

    private Long insertHistoryForPlace(long memberId, long placeId, Instant selectedAt) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO place_search_history (
                    member_id,
                    place_id,
                    place_name,
                    category,
                    address,
                    location,
                    selected_at
                )
                SELECT ?, id, name, category, address, location, ?
                FROM place
                WHERE id = ?
                RETURNING id
                """, Long.class, memberId, Timestamp.from(selectedAt), placeId);
    }

    private Long insertProviderOnlyHistory(String providerPlaceId, Instant selectedAt) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO place_search_history (
                    member_id,
                    place_provider,
                    provider_place_id,
                    place_name,
                    category,
                    address,
                    location,
                    selected_at
                )
                VALUES (
                    ?,
                    'KAKAO',
                    ?,
                    'provider-only 장소',
                    '공원',
                    'provider-only 지번 주소',
                    ST_SetSRID(ST_MakePoint(126.9350, 37.5300), 4326)::geography,
                    ?
                )
                RETURNING id
                """,
                Long.class,
                MEMBER_ID,
                providerPlaceId,
                Timestamp.from(selectedAt)
        );
    }

    private Map<String, Object> historySnapshot(long historyId) {
        return jdbcTemplate.queryForMap("""
                SELECT place_id,
                       place_provider,
                       provider_place_id,
                       place_name,
                       category,
                       address,
                       ST_X(location::geometry) AS longitude,
                       ST_Y(location::geometry) AS latitude,
                       selected_at,
                       created_at,
                       updated_at
                FROM place_search_history
                WHERE id = ?
                """, historyId);
    }

    private long placeCountByProviderPlaceId(String providerPlaceId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM place
                WHERE place_provider = 'KAKAO'
                  AND provider_place_id = ?
                """, Long.class, providerPlaceId);
    }

    private List<Instant> selectedTimes(long memberId) {
        return jdbcTemplate.query("""
                SELECT selected_at
                FROM place_search_history
                WHERE member_id = ?
                ORDER BY selected_at DESC, id DESC
                """,
                (resultSet, rowNumber) -> resultSet.getTimestamp("selected_at").toInstant(),
                memberId
        );
    }

    private long historyCount(long memberId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM place_search_history
                WHERE member_id = ?
                """, Long.class, memberId);
    }

    private boolean historyExists(long historyId) {
        return jdbcTemplate.queryForObject("""
                SELECT EXISTS(
                    SELECT 1
                    FROM place_search_history
                    WHERE id = ?
                )
                """, Boolean.class, historyId);
    }

    private boolean placeExists(long placeId) {
        return jdbcTemplate.queryForObject("""
                SELECT EXISTS(
                    SELECT 1
                    FROM place
                    WHERE id = ?
                )
                """, Boolean.class, placeId);
    }

    private void clearData() {
        jdbcTemplate.update(
                "DELETE FROM place_search_history WHERE member_id IN (?, ?)",
                MEMBER_ID,
                OTHER_MEMBER_ID
        );
        jdbcTemplate.update("""
                DELETE FROM place
                WHERE place_provider = 'KAKAO'
                  AND provider_place_id LIKE ?
                """, PROVIDER_PLACE_ID_PREFIX + "%");
        jdbcTemplate.update(
                "DELETE FROM member WHERE id IN (?, ?)",
                MEMBER_ID,
                OTHER_MEMBER_ID
        );
    }

    private record HistoryFixture(Long placeId, Long historyId) {
    }
}
