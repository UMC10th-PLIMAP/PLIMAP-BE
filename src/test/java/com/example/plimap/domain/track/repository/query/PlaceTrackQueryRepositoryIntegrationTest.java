package com.example.plimap.domain.track.repository.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.plimap.domain.track.dto.PlaceTrackQueryResult;
import com.example.plimap.domain.track.enums.PlaceTrackSort;
import com.example.plimap.support.PostgisContainerConfiguration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
@Transactional
class PlaceTrackQueryRepositoryIntegrationTest {

    @Autowired
    private PlaceTrackQueryRepository placeTrackQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long memberId;
    private Long otherMemberId;
    private Long placeId;
    private int pinMemberSequence;

    @BeforeEach
    void setUp() {
        memberId = insertMember("현재사용자");
        otherMemberId = insertMember("다른사용자");
        placeId = insertPlace();
        pinMemberSequence = 0;
    }

    @Test
    void 인기순은_좋아요_최근_PIN_placeTrackId_순으로_정렬한다() {
        Long leastPopular = insertPlaceTrack(1, "낮은 인기");
        Long olderPopular = insertPlaceTrack(5, "오래된 인기");
        Long newerLowerId = insertPlaceTrack(5, "최근 동률 1");
        Long newerHigherId = insertPlaceTrack(5, "최근 동률 2");

        insertPin(leastPopular, "2026-07-23T00:04:00Z", true, false);
        insertPin(olderPopular, "2026-07-23T00:01:00Z", true, false);
        insertPin(newerLowerId, "2026-07-23T00:03:00Z", true, false);
        insertPin(newerHigherId, "2026-07-23T00:03:00Z", true, false);

        Slice<PlaceTrackQueryResult> result = find(PlaceTrackSort.POPULAR, 0, 20);

        assertThat(result.getContent())
                .extracting(PlaceTrackQueryResult::placeTrackId)
                .containsExactly(
                        newerHigherId,
                        newerLowerId,
                        olderPopular,
                        leastPopular
                );
    }

    @Test
    void 최신순은_최근_PIN과_placeTrackId_순으로_정렬한다() {
        Long older = insertPlaceTrack(100, "오래된 곡");
        Long newerLowerId = insertPlaceTrack(1, "최근 곡 1");
        Long newerHigherId = insertPlaceTrack(0, "최근 곡 2");

        insertPin(older, "2026-07-23T00:01:00Z", true, false);
        insertPin(newerLowerId, "2026-07-23T00:03:00Z", true, false);
        insertPin(newerHigherId, "2026-07-23T00:03:00Z", true, false);

        Slice<PlaceTrackQueryResult> result = find(PlaceTrackSort.LATEST, 0, 20);

        assertThat(result.getContent())
                .extracting(PlaceTrackQueryResult::placeTrackId)
                .containsExactly(newerHigherId, newerLowerId, older);
    }

    @Test
    void 유효한_공개_PIN만_집계하고_삭제된_PlaceTrack을_제외한다() {
        Long active = insertPlaceTrack(3, "활성 곡");
        Long deleted = insertPlaceTrack(10, "삭제 곡");
        insertPin(active, "2026-07-23T00:01:00Z", true, false);
        insertPin(active, "2026-07-23T00:02:00Z", true, false);
        insertPin(active, "2026-07-23T00:03:00Z", true, true);
        insertPin(active, "2026-07-23T00:04:00Z", false, false);
        insertPin(deleted, "2026-07-23T00:05:00Z", true, false);
        deletePlaceTrack(deleted);

        Slice<PlaceTrackQueryResult> result = find(PlaceTrackSort.POPULAR, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().placeTrackId()).isEqualTo(active);
        assertThat(result.getContent().getFirst().pinCount()).isEqualTo(2);
    }

    @Test
    void 현재_사용자의_좋아요_여부를_조회한다() {
        Long liked = insertPlaceTrack(5, "좋아요 곡");
        Long notLiked = insertPlaceTrack(4, "미좋아요 곡");
        insertPin(liked, "2026-07-23T00:02:00Z", true, false);
        insertPin(notLiked, "2026-07-23T00:01:00Z", true, false);
        insertPlaceTrackLike(liked, memberId);
        insertPlaceTrackLike(notLiked, otherMemberId);

        Slice<PlaceTrackQueryResult> result = find(PlaceTrackSort.POPULAR, 0, 20);

        assertThat(result.getContent())
                .extracting(PlaceTrackQueryResult::liked)
                .containsExactly(true, false);
    }

    @Test
    void size보다_한_건_더_조회해_다음_페이지를_판단한다() {
        Long first = insertPlaceTrack(3, "첫 곡");
        Long second = insertPlaceTrack(2, "둘째 곡");
        Long third = insertPlaceTrack(1, "셋째 곡");
        insertPin(first, "2026-07-23T00:03:00Z", true, false);
        insertPin(second, "2026-07-23T00:02:00Z", true, false);
        insertPin(third, "2026-07-23T00:01:00Z", true, false);

        Slice<PlaceTrackQueryResult> firstPage =
                find(PlaceTrackSort.POPULAR, 0, 2);
        Slice<PlaceTrackQueryResult> secondPage =
                find(PlaceTrackSort.POPULAR, 1, 2);

        assertThat(firstPage.getContent())
                .extracting(PlaceTrackQueryResult::placeTrackId)
                .containsExactly(first, second);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.getContent())
                .extracting(PlaceTrackQueryResult::placeTrackId)
                .containsExactly(third);
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void 장소_북마크_여부를_조회한다() {
        assertThat(placeTrackQueryRepository.existsPlaceBookmark(placeId, memberId))
                .isFalse();

        jdbcTemplate.update(
                "INSERT INTO place_bookmark (place_id, member_id) VALUES (?, ?)",
                placeId,
                memberId
        );

        assertThat(placeTrackQueryRepository.existsPlaceBookmark(placeId, memberId))
                .isTrue();
        assertThat(placeTrackQueryRepository.existsPlaceBookmark(placeId, otherMemberId))
                .isFalse();
    }

    private Slice<PlaceTrackQueryResult> find(
            PlaceTrackSort sort,
            int page,
            int size
    ) {
        return placeTrackQueryRepository.findPlaceTracks(
                placeId,
                memberId,
                sort,
                PageRequest.of(page, size)
        );
    }

    private Long insertMember(String nickname) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO member (nickname, status)
                VALUES (?, 'ACTIVE')
                RETURNING id
                """, Long.class, nickname);
    }

    private Long insertPlace() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO place (name, address, source, location)
                VALUES (
                    '테스트 장소',
                    '테스트 주소',
                    'MAP_SELECTION',
                    ST_SetSRID(ST_MakePoint(127.0, 37.0), 4326)::geography
                )
                RETURNING id
                """, Long.class);
    }

    private Long insertPlaceTrack(int likeCount, String title) {
        Long trackId = jdbcTemplate.queryForObject("""
                INSERT INTO track (
                    provider,
                    provider_track_id,
                    title,
                    artist_name,
                    album_image_url
                )
                VALUES ('YOUTUBE', ?, ?, '아티스트', ?)
                RETURNING id
                """, Long.class, "video-" + title, title, "https://image/" + title);
        return jdbcTemplate.queryForObject("""
                INSERT INTO place_track (place_id, track_id, like_count)
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, placeId, trackId, likeCount);
    }

    private void insertPin(
            Long placeTrackId,
            String createdAt,
            boolean feedPublic,
            boolean deleted
    ) {
        jdbcTemplate.update("""
                INSERT INTO pin (
                    member_id,
                    place_id,
                    place_track_id,
                    clip_start_ms,
                    introduction,
                    is_feed_public,
                    created_at,
                    deleted_at
                )
                VALUES (?, ?, ?, 0, '', ?, ?, ?)
                """,
                insertMember("핀사용자" + pinMemberSequence++),
                placeId,
                placeTrackId,
                feedPublic,
                OffsetDateTime.parse(createdAt),
                deleted ? OffsetDateTime.parse("2026-07-23T01:00:00Z") : null
        );
    }

    private void insertPlaceTrackLike(Long placeTrackId, Long likedMemberId) {
        jdbcTemplate.update(
                "INSERT INTO place_track_like (place_track_id, member_id) VALUES (?, ?)",
                placeTrackId,
                likedMemberId
        );
    }

    private void deletePlaceTrack(Long placeTrackId) {
        jdbcTemplate.update(
                "UPDATE place_track SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?",
                placeTrackId
        );
    }
}
