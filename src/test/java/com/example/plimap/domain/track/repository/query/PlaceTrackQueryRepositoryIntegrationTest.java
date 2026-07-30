package com.example.plimap.domain.track.repository.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.plimap.domain.track.dto.LikedPlaceTrackQueryResult;
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
    void 공개_PIN과_비공개_PIN을_모두_집계하고_삭제된_PIN은_제외한다() {
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
        assertThat(result.getContent().getFirst().pinCount()).isEqualTo(3);
    }

    @Test
    void 비공개_PIN만_존재하는_PlaceTrack도_목록에_반환한다() {
        Long privateOnly = insertPlaceTrack(1, "비공개 곡");
        insertPin(privateOnly, "2026-07-23T00:01:00Z", false, false);

        Slice<PlaceTrackQueryResult> result = find(PlaceTrackSort.POPULAR, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().placeTrackId()).isEqualTo(privateOnly);
        assertThat(result.getContent().getFirst().pinCount()).isEqualTo(1);
    }

    @Test
    void 비공개_PIN이_더_최근이면_최신순에_반영한다() {
        Long publicOlder = insertPlaceTrack(1, "공개 PIN 곡");
        Long privateNewer = insertPlaceTrack(1, "비공개 PIN 곡");
        insertPin(publicOlder, "2026-07-23T00:01:00Z", true, false);
        insertPin(privateNewer, "2026-07-23T00:02:00Z", false, false);

        Slice<PlaceTrackQueryResult> result = find(PlaceTrackSort.LATEST, 0, 20);

        assertThat(result.getContent())
                .extracting(PlaceTrackQueryResult::placeTrackId)
                .containsExactly(privateNewer, publicOlder);
    }

    @Test
    void 인기순_좋아요가_같으면_비공개_PIN_생성_시각을_보조_정렬에_반영한다() {
        Long publicOlder = insertPlaceTrack(5, "공개 PIN 인기 곡");
        Long privateNewer = insertPlaceTrack(5, "비공개 PIN 인기 곡");
        insertPin(publicOlder, "2026-07-23T00:01:00Z", true, false);
        insertPin(privateNewer, "2026-07-23T00:02:00Z", false, false);

        Slice<PlaceTrackQueryResult> result = find(PlaceTrackSort.POPULAR, 0, 20);

        assertThat(result.getContent())
                .extracting(PlaceTrackQueryResult::placeTrackId)
                .containsExactly(privateNewer, publicOlder);
    }

    @Test
    void 삭제된_PIN은_목록과_PIN수와_최신_생성_시각에서_제외한다() {
        Long activeWithDeletedNewer = insertPlaceTrack(1, "삭제 PIN 포함 곡");
        Long activeNewer = insertPlaceTrack(1, "활성 최신 곡");
        Long deletedOnly = insertPlaceTrack(1, "삭제 PIN 전용 곡");
        insertPin(
                activeWithDeletedNewer,
                "2026-07-23T00:01:00Z",
                true,
                false
        );
        insertPin(
                activeWithDeletedNewer,
                "2026-07-23T00:03:00Z",
                false,
                true
        );
        insertPin(activeNewer, "2026-07-23T00:02:00Z", false, false);
        insertPin(deletedOnly, "2026-07-23T00:04:00Z", true, true);

        Slice<PlaceTrackQueryResult> result = find(PlaceTrackSort.LATEST, 0, 20);

        assertThat(result.getContent())
                .extracting(PlaceTrackQueryResult::placeTrackId)
                .containsExactly(activeNewer, activeWithDeletedNewer);
        assertThat(result.getContent().get(1).pinCount()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(PlaceTrackQueryResult::placeTrackId)
                .doesNotContain(deletedOnly);
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
    void 현재_사용자가_좋아요한_PlaceTrack만_최신_좋아요순으로_조회한다() {
        Long older = insertPlaceTrack(5, "오래된 좋아요 곡");
        Long newer = insertPlaceTrack(12, "최근 좋아요 곡");
        Long otherMemberLiked = insertPlaceTrack(20, "다른 사용자 곡");
        insertPlaceTrackLike(
                older,
                memberId,
                "2026-07-23T00:01:00Z"
        );
        insertPlaceTrackLike(
                newer,
                memberId,
                "2026-07-23T00:02:00Z"
        );
        insertPlaceTrackLike(
                otherMemberLiked,
                otherMemberId,
                "2026-07-23T00:03:00Z"
        );

        Slice<LikedPlaceTrackQueryResult> result = findLiked(0, 20);

        assertThat(result.getContent())
                .extracting(LikedPlaceTrackQueryResult::placeTrackId)
                .containsExactly(newer, older);
        LikedPlaceTrackQueryResult first = result.getContent().getFirst();
        assertThat(first.trackName()).isEqualTo("최근 좋아요 곡");
        assertThat(first.artistName()).isEqualTo("아티스트");
        assertThat(first.artworkUrl()).isEqualTo("https://image/최근 좋아요 곡");
        assertThat(first.likeCount()).isEqualTo(12);
    }

    @Test
    void 좋아요한_PlaceTrack이_없으면_빈_Slice를_반환한다() {
        Slice<LikedPlaceTrackQueryResult> result = findLiked(0, 20);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void 좋아요를_취소한_PlaceTrack은_목록에_반환하지_않는다() {
        Long placeTrackId = insertPlaceTrack(3, "좋아요 취소 곡");
        insertPlaceTrackLike(
                placeTrackId,
                memberId,
                "2026-07-23T00:01:00Z"
        );
        jdbcTemplate.update("""
                DELETE FROM place_track_like
                WHERE place_track_id = ?
                  AND member_id = ?
                """,
                placeTrackId,
                memberId
        );

        Slice<LikedPlaceTrackQueryResult> result = findLiked(0, 20);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void 삭제된_PlaceTrack은_좋아요_목록에서_제외한다() {
        Long active = insertPlaceTrack(3, "활성 좋아요 곡");
        Long deleted = insertPlaceTrack(4, "삭제된 좋아요 곡");
        insertPlaceTrackLike(active, memberId, "2026-07-23T00:01:00Z");
        insertPlaceTrackLike(deleted, memberId, "2026-07-23T00:02:00Z");
        deletePlaceTrack(deleted);

        Slice<LikedPlaceTrackQueryResult> result = findLiked(0, 20);

        assertThat(result.getContent())
                .extracting(LikedPlaceTrackQueryResult::placeTrackId)
                .containsExactly(active);
    }

    @Test
    void 삭제된_Place의_PlaceTrack은_좋아요_목록에서_제외한다() {
        Long placeTrackId = insertPlaceTrack(3, "삭제 장소의 곡");
        insertPlaceTrackLike(
                placeTrackId,
                memberId,
                "2026-07-23T00:01:00Z"
        );
        jdbcTemplate.update(
                "UPDATE place SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?",
                placeId
        );

        Slice<LikedPlaceTrackQueryResult> result = findLiked(0, 20);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void 동일한_Track의_서로_다른_PlaceTrack을_각각_반환한다() {
        Long trackId = insertTrack("공통 곡");
        Long otherPlaceId = insertPlace("다른 테스트 장소");
        Long firstPlaceTrack = insertPlaceTrack(placeId, trackId, 2);
        Long secondPlaceTrack = insertPlaceTrack(otherPlaceId, trackId, 7);
        insertPlaceTrackLike(
                firstPlaceTrack,
                memberId,
                "2026-07-23T00:01:00Z"
        );
        insertPlaceTrackLike(
                secondPlaceTrack,
                memberId,
                "2026-07-23T00:02:00Z"
        );

        Slice<LikedPlaceTrackQueryResult> result = findLiked(0, 20);

        assertThat(result.getContent())
                .extracting(LikedPlaceTrackQueryResult::placeTrackId)
                .containsExactly(secondPlaceTrack, firstPlaceTrack);
        assertThat(result.getContent())
                .extracting(LikedPlaceTrackQueryResult::trackName)
                .containsExactly("공통 곡", "공통 곡");
        assertThat(result.getContent())
                .extracting(LikedPlaceTrackQueryResult::likeCount)
                .containsExactly(7, 2);
    }

    @Test
    void 좋아요_목록은_size보다_한_건_더_조회해_hasNext를_계산한다() {
        Long oldest = insertPlaceTrack(1, "첫 번째 좋아요");
        Long middle = insertPlaceTrack(2, "두 번째 좋아요");
        Long newest = insertPlaceTrack(3, "세 번째 좋아요");
        insertPlaceTrackLike(oldest, memberId, "2026-07-23T00:01:00Z");
        insertPlaceTrackLike(middle, memberId, "2026-07-23T00:02:00Z");
        insertPlaceTrackLike(newest, memberId, "2026-07-23T00:03:00Z");

        Slice<LikedPlaceTrackQueryResult> firstPage = findLiked(0, 2);
        Slice<LikedPlaceTrackQueryResult> secondPage = findLiked(1, 2);

        assertThat(firstPage.getContent())
                .extracting(LikedPlaceTrackQueryResult::placeTrackId)
                .containsExactly(newest, middle);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.getContent())
                .extracting(LikedPlaceTrackQueryResult::placeTrackId)
                .containsExactly(oldest);
        assertThat(secondPage.hasNext()).isFalse();
    }

    private Slice<LikedPlaceTrackQueryResult> findLiked(int page, int size) {
        return placeTrackQueryRepository.findLikedPlaceTracks(
                memberId,
                PageRequest.of(page, size)
        );
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
        return insertPlace("테스트 장소");
    }

    private Long insertPlace(String placeName) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO place (name, address, source, location)
                VALUES (
                    ?,
                    '테스트 주소',
                    'MAP_SELECTION',
                    ST_SetSRID(ST_MakePoint(127.0, 37.0), 4326)::geography
                )
                RETURNING id
                """, Long.class, placeName);
    }

    private Long insertPlaceTrack(int likeCount, String title) {
        Long trackId = insertTrack(title);
        return insertPlaceTrack(placeId, trackId, likeCount);
    }

    private Long insertTrack(String title) {
        return jdbcTemplate.queryForObject("""
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
    }

    private Long insertPlaceTrack(
            Long targetPlaceId,
            Long trackId,
            int likeCount
    ) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO place_track (place_id, track_id, like_count)
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, targetPlaceId, trackId, likeCount);
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

    private void insertPlaceTrackLike(
            Long placeTrackId,
            Long likedMemberId,
            String createdAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO place_track_like (
                    place_track_id,
                    member_id,
                    created_at
                )
                VALUES (?, ?, ?)
                """,
                placeTrackId,
                likedMemberId,
                OffsetDateTime.parse(createdAt)
        );
    }

    private void deletePlaceTrack(Long placeTrackId) {
        jdbcTemplate.update(
                "UPDATE place_track SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?",
                placeTrackId
        );
    }
}
