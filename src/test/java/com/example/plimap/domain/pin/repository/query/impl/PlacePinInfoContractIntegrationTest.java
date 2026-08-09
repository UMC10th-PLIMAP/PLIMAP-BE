package com.example.plimap.domain.pin.repository.query.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.repository.PinRepository;
import com.example.plimap.domain.pin.repository.query.PinQueryRepository;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.Track;
import com.example.plimap.domain.track.repository.PlaceTrackRepository;
import com.example.plimap.domain.track.repository.TrackRepository;
import com.example.plimap.support.PostgisContainerConfiguration;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
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
class PlacePinInfoContractIntegrationTest {

    @Autowired
    private PinQueryRepository pinQueryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private PlaceTrackRepository placeTrackRepository;

    @Autowired
    private PinRepository pinRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Member firstWriter;
    private Place place;
    private Pin firstPin;
    private Pin secondPin;

    @BeforeEach
    void setUp() {
        firstWriter = memberRepository.save(member("첫작성자"));
        Member secondWriter = memberRepository.save(member("다음작성자"));
        place = placeRepository.save(place());
        Track track = trackRepository.save(Track.create(
                "SPOTIFY",
                "place-pin-info-track",
                "테스트 곡",
                "테스트 가수",
                null,
                "테스트 앨범",
                null,
                null
        ));
        PlaceTrack placeTrack = placeTrackRepository.save(PlaceTrack.create(place, track));
        firstPin = pinRepository.save(pin(firstWriter, placeTrack));
        secondPin = pinRepository.save(pin(secondWriter, placeTrack));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void 최초_활성_PIN의_생성_시각이_같으면_작은_PIN_ID의_작성자_닉네임을_반환한다() {
        Instant sameCreatedAt = Instant.parse("2026-08-08T00:00:00Z");
        jdbcTemplate.update(
                "UPDATE pin SET created_at = ? WHERE id IN (?, ?)",
                Timestamp.from(sameCreatedAt),
                firstPin.getId(),
                secondPin.getId()
        );

        Map<Long, PlacePinInfo> result =
                pinQueryRepository.findPinInfosByPlaceIds(List.of(place.getId()));

        assertThat(firstPin.getId()).isLessThan(secondPin.getId());
        assertThat(result.get(place.getId()).firstPinCreatorNickname()).isEqualTo("첫작성자");
    }

    @Test
    void 최초_활성_PIN_작성자가_탈퇴한_회원이면_플리맵사용자를_반환한다() {
        Member writer = memberRepository.findById(firstWriter.getId()).orElseThrow();
        writer.withdrawVoluntarily();
        entityManager.flush();
        entityManager.clear();

        Map<Long, PlacePinInfo> result =
                pinQueryRepository.findPinInfosByPlaceIds(List.of(place.getId()));

        assertThat(result.get(place.getId()).firstPinCreatorNickname())
                .isEqualTo("플리맵사용자");
    }

    private Member member(String nickname) {
        return Member.builder()
                .name(nickname)
                .nickname(nickname)
                .build();
    }

    private Place place() {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        return Place.builder()
                .name("테스트 장소")
                .address("서울특별시 테스트구")
                .source(PlaceSource.MAP_SELECTION)
                .location(geometryFactory.createPoint(new Coordinate(126.9326, 37.5283)))
                .build();
    }

    private Pin pin(Member writer, PlaceTrack placeTrack) {
        return Pin.builder()
                .member(writer)
                .place(place)
                .placeTrack(placeTrack)
                .clipStartMs(0)
                .introduction("테스트 PIN")
                .isFeedPublic(false)
                .build();
    }
}
