package com.example.plimap.domain.pin.repository.query.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
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

    @BeforeEach
    void setup() {
        Member member = Member.builder()
                .name("이서윤")
                .nickname("이서")
                .introduction("안녕하세요")
                .profileImageObjectKey("image_url")
                .build();


        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        Point point = geometryFactory.createPoint(
                new Coordinate(126.9326, 37.5283)
        );
        Place place = Place.builder()
                .name("한강공원")
                .address("서울 강남구 압구정동 387")
                .source(PlaceSource.MAP_SELECTION)
                .location(point)
                .build();

        Track track = Track.builder()
                .title("title")
                .previewUrl("preview")
                .albumImageUrl("album")
                .artistName("artist")
                .previewUrl("url_test")
                .provider("provider")
                .providerTrackId("providerTrackId")
                .build();

        PlaceTrack placeTrack = PlaceTrack.builder()
                .place(place)
                .track(track)
                .build();

        Pin pin = Pin.builder()
                .member(member)
                .place(place)
                .placeTrack(placeTrack)
                .clipStartMs(70000)
                .introduction("good")
                .isFeedPublic(true)
                .build();

        memberRepository.save(member);
        placeRepository.save(place);
        trackRepository.save(track);
        placeTrackRepository.save(placeTrack);
        pinRepository.save(pin);

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

}