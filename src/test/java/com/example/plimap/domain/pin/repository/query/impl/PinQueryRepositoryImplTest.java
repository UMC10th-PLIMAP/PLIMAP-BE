package com.example.plimap.domain.pin.repository.query.impl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
@Transactional
class PinQueryRepositoryImplTest {

    @Autowired
    private PinQueryRepository pinQueryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private PinRepository pinRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private PlaceTrackRepository placeTrackRepository;

    @Autowired
    EntityManager entityManager;

    Pin pin1;
    private Place place1;
    private Place place2;
    private Place place3;


    @BeforeEach
    void setup() {
        Member member1 = Member.builder()
                .name("이서윤")
                .nickname("이서")
                .introduction("안녕하세요")
                .profileImageObjectKey("image_url")
                .build();

        Member member2 = Member.builder()
                .name("홍길동")
                .nickname("동길")
                .introduction("안녕하세요")
                .profileImageObjectKey("image_url")
                .build();

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        place1 = Place.builder()
                .name("여의도 한강공원")
                .address("서울 영등포구 여의동로 330")
                .source(PlaceSource.MAP_SELECTION)
                .location(geometryFactory.createPoint(
                        new Coordinate(126.9326, 37.5283)
                ))
                .build();

        place2 = Place.builder()
                .name("서울숲")
                .address("서울 성동구 뚝섬로 273")
                .source(PlaceSource.MAP_SELECTION)
                .location(geometryFactory.createPoint(
                        new Coordinate(127.0372, 37.5446)
                ))
                .build();

        place3 = Place.builder()
                .name("남산서울타워")
                .address("서울 용산구 남산공원길 105")
                .source(PlaceSource.MAP_SELECTION)
                .location(geometryFactory.createPoint(
                        new Coordinate(126.9882, 37.5512)
                ))
                .build();

        Track track = Track.create(
                "provider",
                "providerTrackId",
                "title",
                "artist",
                null,
                "album",
                "url_test",
                null
        );

        PlaceTrack placeTrack1 = PlaceTrack.create(place1, track);

        PlaceTrack placeTrack2 = PlaceTrack.create(place2, track);

        // place1 - member1, member2가 pin 등록
        pin1 = Pin.builder()
                .member(member1)
                .place(place1)
                .placeTrack(placeTrack1)
                .clipStartMs(70000)
                .introduction("good")
                .isFeedPublic(true)
                .build();

        Pin pin2 = Pin.builder()
                .member(member2)
                .place(place1)
                .placeTrack(placeTrack1)
                .clipStartMs(70000)
                .introduction("good")
                .isFeedPublic(true)
                .build();

        // place2 - member2가 pin 등록
        Pin pin3 = Pin.builder()
                .member(member2)
                .place(place2)
                .placeTrack(placeTrack2)
                .clipStartMs(70000)
                .introduction("good")
                .isFeedPublic(true)
                .build();

        memberRepository.saveAll(List.of(member1, member2));
        placeRepository.saveAll(List.of(place1, place2, place3));
        trackRepository.save(track);
        placeTrackRepository.saveAll(List.of(placeTrack1, placeTrack2));
        pinRepository.saveAll(List.of(pin1, pin2, pin3));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void 현위치로부터_20m_이내_핀이_있으면_거리를_반환한다() {
        Optional<Double> distance =
                pinQueryRepository.findNearestActivePinWithin20m(37.5282,126.9326);

        assertThat(distance).isPresent();
        assertThat(distance.get()).isLessThan(20);
    }

    @Test
    void 현위치로부터_20m_이내_핀이_없으면_OptionalEmpty를_반환한다() {
        Optional<Double> distance =
                pinQueryRepository.findNearestActivePinWithin20m(37.6000,127.1000);

        assertThat(distance).isEmpty();
    }

    @Test
    void 장소_ID_목록에_해당하는_장소의_핀등록_여부와_첫_등록자_닉네임을_반환한다() {
        List<Long> placeIdList = List.of(
                place1.getId(),
                place2.getId(),
                place3.getId()
        );
        Map<Long, PlacePinInfo> result = pinQueryRepository.findPinInfosByPlaceIds(placeIdList);

        assertThat(result.get(place1.getId()).hasPin()).isTrue();
        assertThat(result.get(place1.getId()).firstPinCreatorNickname()).isEqualTo("이서");
        assertThat(result.get(place2.getId()).hasPin()).isTrue();
        assertThat(result.get(place2.getId()).firstPinCreatorNickname()).isEqualTo("동길");
        assertThat(result.get(place3.getId()).hasPin()).isFalse();
        assertThat(result.get(place3.getId()).firstPinCreatorNickname()).isNull();
    }

    @Test
    void 최초_등록_핀이_삭제되면_다음_활성_핀_등록자의_닉네임을_반환한다() {
        List<Long> placeIdList = List.of(
                place1.getId(),
                place2.getId(),
                place3.getId()
        );
        Pin managedPin = pinRepository.findById(pin1.getId()).orElseThrow();
        managedPin.delete();
        entityManager.flush();
        entityManager.clear();

        Map<Long, PlacePinInfo> result = pinQueryRepository.findPinInfosByPlaceIds(placeIdList);

        assertThat(result.get(place1.getId()).hasPin()).isTrue();
        assertThat(result.get(place1.getId()).firstPinCreatorNickname()).isEqualTo("동길");
    }

}
