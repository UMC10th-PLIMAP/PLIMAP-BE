package com.example.plimap.domain.place.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.plimap.domain.pin.repository.PinRepository;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.repository.PlaceBookmarkRepository;
import com.example.plimap.domain.place.repository.PlaceRepository;
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
class PlaceBookmarkListIntegrationTest {

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
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private PinQueryService pinQueryService;

    @Test
    void 최초_활성_PIN을_배치_병합하고_조회_전후_데이터를_변경하지_않는다() {
        Long memberId = insertMember("조회회원");
        Long firstPinMemberId = insertMember("비공개최초");
        Long tiedPinMemberId = insertMember("동시공개");
        Long laterPinMemberId = insertMember("공개후속");
        Long deletedPinMemberId = insertMember("삭제된최초");
        Long pinPlaceId = insertPlace("PIN 있는 장소", 100.0);
        Long noPinPlaceId = insertPlace("PIN 없는 장소", 200.0);
        insertBookmark(memberId, pinPlaceId);
        insertBookmark(memberId, noPinPlaceId);
        Long placeTrackId = insertPlaceTrack(pinPlaceId);
        insertPin(deletedPinMemberId, pinPlaceId, placeTrackId, false, 30, true);
        insertPin(firstPinMemberId, pinPlaceId, placeTrackId, false, 20, false);
        insertPin(tiedPinMemberId, pinPlaceId, placeTrackId, true, 20, false);
        insertPin(laterPinMemberId, pinPlaceId, placeTrackId, true, 10, false);
        long placeCount = placeRepository.count();
        long bookmarkCount = placeBookmarkRepository.count();
        long pinCount = pinRepository.count();

        PlaceResponse.BookmarkListResult result = placeQueryService.getPlaceBookmarks(
                memberId,
                LATITUDE,
                LONGITUDE
        );

        assertThat(result.items()).containsExactly(
                new PlaceResponse.BookmarkListItem(
                        pinPlaceId,
                        "PIN 있는 장소",
                        "비공개최초",
                        100
                ),
                new PlaceResponse.BookmarkListItem(
                        noPinPlaceId,
                        "PIN 없는 장소",
                        null,
                        200
                )
        );
        verify(pinQueryService, times(1))
                .findPinInfosByPlaceIds(List.of(pinPlaceId, noPinPlaceId));
        assertThat(placeRepository.count()).isEqualTo(placeCount);
        assertThat(placeBookmarkRepository.count()).isEqualTo(bookmarkCount);
        assertThat(pinRepository.count()).isEqualTo(pinCount);
    }

    private Long insertMember(String nickname) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO member (nickname, name)
                VALUES (?, '테스터')
                RETURNING id
                """, Long.class, nickname);
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

    private void insertBookmark(Long memberId, Long placeId) {
        jdbcTemplate.update("""
                INSERT INTO place_bookmark (place_id, member_id, created_at, updated_at)
                VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, placeId, memberId);
    }

    private Long insertPlaceTrack(Long placeId) {
        Long trackId = jdbcTemplate.queryForObject("""
                INSERT INTO track (provider, provider_track_id, title, artist_name)
                VALUES ('TEST', ?, '테스트 곡', '테스트 가수')
                RETURNING id
                """, Long.class, "track-" + placeId);
        return jdbcTemplate.queryForObject("""
                INSERT INTO place_track (place_id, track_id)
                VALUES (?, ?)
                RETURNING id
                """, Long.class, placeId, trackId);
    }

    private void insertPin(
            Long memberId,
            Long placeId,
            Long placeTrackId,
            boolean feedPublic,
            int ageSeconds,
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
                    updated_at,
                    deleted_at
                )
                VALUES (
                    ?, ?, ?, 0, '', ?,
                    CURRENT_TIMESTAMP - (? * INTERVAL '1 second'),
                    CURRENT_TIMESTAMP,
                    CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END
                )
                """, memberId, placeId, placeTrackId, feedPublic, ageSeconds, deleted);
    }
}
