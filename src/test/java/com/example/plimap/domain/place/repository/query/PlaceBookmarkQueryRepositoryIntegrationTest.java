package com.example.plimap.domain.place.repository.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.plimap.domain.place.dto.NearbyBookmarkedPlace;
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
class PlaceBookmarkQueryRepositoryIntegrationTest {

    private static final double LATITUDE = 37.5283;
    private static final double LONGITUDE = 126.9326;

    @Autowired
    private PlaceBookmarkQueryRepository placeBookmarkQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 저장한_활성_장소를_거리순으로_조회하고_9개_미만이면_모두_반환한다() {
        Long memberId = insertMember();
        Long fartherId = insertPlace("먼 장소", 300.0, 0.0, false);
        Long nearerId = insertPlace("가까운 장소", 100.0, 0.0, false);
        insertBookmark(memberId, fartherId, 10);
        insertBookmark(memberId, nearerId, 20);

        List<NearbyBookmarkedPlace> result =
                placeBookmarkQueryRepository.findNearbyActiveBookmarks(
                        memberId,
                        LATITUDE,
                        LONGITUDE
                );

        assertThat(result).containsExactly(
                new NearbyBookmarkedPlace(nearerId, "가까운 장소", 100),
                new NearbyBookmarkedPlace(fartherId, "먼 장소", 300)
        );
    }

    @Test
    void 같은_거리에서는_북마크_최신순과_placeId순으로_정렬한다() {
        Long memberId = insertMember();
        Long firstId = insertPlace("첫 장소", 100.0, 0.0, false);
        Long secondId = insertPlace("두 번째 장소", 100.0, 90.0, false);
        Long newestId = insertPlace("최신 북마크 장소", 100.0, 180.0, false);
        insertBookmark(memberId, firstId, 60);
        insertBookmark(memberId, secondId, 60);
        insertBookmark(memberId, newestId, 0);

        List<NearbyBookmarkedPlace> result =
                placeBookmarkQueryRepository.findNearbyActiveBookmarks(
                        memberId,
                        LATITUDE,
                        LONGITUDE
                );

        assertThat(result).extracting(NearbyBookmarkedPlace::placeId)
                .containsExactly(newestId, firstId, secondId);
    }

    @Test
    void 정확히_500m는_포함하고_500m를_초과하면_제외한다() {
        Long memberId = insertMember();
        Long boundaryId = insertPlace("경계 장소", 500.0, 0.0, false);
        Long outsideId = insertPlace("경계 밖 장소", 500.01, 0.0, false);
        insertBookmark(memberId, boundaryId, 0);
        insertBookmark(memberId, outsideId, 0);

        List<NearbyBookmarkedPlace> result =
                placeBookmarkQueryRepository.findNearbyActiveBookmarks(
                        memberId,
                        LATITUDE,
                        LONGITUDE
                );

        assertThat(distanceFrom(boundaryId)).isEqualTo(500.0);
        assertThat(distanceFrom(outsideId)).isGreaterThan(500.0);
        assertThat(result).containsExactly(
                new NearbyBookmarkedPlace(boundaryId, "경계 장소", 500)
        );
    }

    @Test
    void 정렬한_뒤_최대_9개만_반환한다() {
        Long memberId = insertMember();
        List<Long> placeIds = new ArrayList<>();
        for (int index = 1; index <= 10; index++) {
            Long placeId = insertPlace(
                    "장소 " + index,
                    index * 10.0,
                    0.0,
                    false
            );
            placeIds.add(placeId);
            insertBookmark(memberId, placeId, 0);
        }

        List<NearbyBookmarkedPlace> result =
                placeBookmarkQueryRepository.findNearbyActiveBookmarks(
                        memberId,
                        LATITUDE,
                        LONGITUDE
                );

        assertThat(result).hasSize(9);
        assertThat(result).extracting(NearbyBookmarkedPlace::placeId)
                .containsExactlyElementsOf(placeIds.subList(0, 9));
    }

    @Test
    void 다른_회원의_북마크와_Soft_Delete된_Place를_제외한다() {
        Long memberId = insertMember();
        Long otherMemberId = insertMember();
        Long activeId = insertPlace("내 활성 장소", 200.0, 0.0, false);
        Long otherMemberPlaceId = insertPlace("다른 회원 장소", 50.0, 0.0, false);
        Long deletedId = insertPlace("삭제 장소", 20.0, 0.0, true);
        insertBookmark(memberId, activeId, 0);
        insertBookmark(otherMemberId, otherMemberPlaceId, 0);
        insertBookmark(memberId, deletedId, 0);

        List<NearbyBookmarkedPlace> result =
                placeBookmarkQueryRepository.findNearbyActiveBookmarks(
                        memberId,
                        LATITUDE,
                        LONGITUDE
                );

        assertThat(result).containsExactly(
                new NearbyBookmarkedPlace(activeId, "내 활성 장소", 200)
        );
    }

    private Long insertMember() {
        return jdbcTemplate.queryForObject(
                "INSERT INTO member DEFAULT VALUES RETURNING id",
                Long.class
        );
    }

    private Long insertPlace(
            String name,
            double distanceMeters,
            double bearingDegrees,
            boolean deleted
    ) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO place (
                    name,
                    address,
                    source,
                    location,
                    deleted_at
                )
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

    private void insertBookmark(Long memberId, Long placeId, int ageSeconds) {
        jdbcTemplate.update("""
                INSERT INTO place_bookmark (place_id, member_id, created_at, updated_at)
                VALUES (
                    ?,
                    ?,
                    CURRENT_TIMESTAMP - (? * INTERVAL '1 second'),
                    CURRENT_TIMESTAMP
                )
                """, placeId, memberId, ageSeconds);
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
