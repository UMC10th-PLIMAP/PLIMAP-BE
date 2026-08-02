package com.example.plimap.domain.pin.repository.query.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.enums.ClusterLevel;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
@Transactional
class ClusterPinListTest {

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

    Pin pin1, pin2, pin3, pin4, pin5, pin6, pin7, pin8, pin9, pin10, pin11;
    Place place1, place2, place3, place4, place5, place6;
    Member member1, member2, member3;
    PlaceTrack placeTrack1, placeTrack2, placeTrack3, placeTrack4, placeTrack5, placeTrack6, placeTrack7, placeTrack8;
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private PinQueryRepository pinQueryRepository;

    @BeforeEach
    void setup() {
        member1 = createMember("이서윤", "이서");
        member2 = createMember("홍길동", "동길");
        member3 = createMember("테스트", "테스트");

        // REGION2 : 성남시
        place1 = createPlace(
                "판교역",
                "경기도 성남시 분당구 판교역로",
                "경기도",
                "성남시",
                "백현동",
                127.1112,
                37.3947
        );

        place2 = createPlace(
                "율동공원",
                "경기도 성남시 분당구",
                "경기도",
                "성남시",
                "서현동",
                127.1483,
                37.3820
        );

        // REGION2 : 수원시
        place3 = createPlace(
                "광교호수공원",
                "경기도 수원시 영통구",
                "경기도",
                "수원시",
                "영통구",
                127.0615,
                37.2865
        );

        // REGION1 fallback 테스트용(세종)
        place4 = createPlace(
                "의당면",
                "세종특별자치시 의당면",
                "세종특별자치시",
                null,
                "의당면",
                127.2587,
                36.5593
        );

        // REGION1 fallback 테스트용(세종)
        place5 = createPlace(
                "연서면",
                "세종특별자치시 연서면",
                "세종특별자치시",
                null,
                "연서면",
                127.2810,
                36.5900
        );

        // REGION3 테스트용(서울)
        place6 = createPlace(
                "서울숲",
                "서울특별시 성동구 성수동",
                "서울특별시",
                "성동구",
                "성수동",
                127.0372,
                37.5446
        );

        Track track1 = Track.create(
                "provider",
                "providerTrackId",
                "title",
                "artist",
                null,
                "album",
                "url_test",
                null
        );

        Track track2 = Track.create(
                "provider2",
                "providerTrackId2",
                "title",
                "artist",
                null,
                "album2",
                "url_test",
                null
        );

        Track track3 = Track.create(
                "provider3",
                "providerTrackId3",
                "title",
                "artist",
                null,
                "album3",
                "url_test",
                null
        );

        placeTrack1 = PlaceTrack.create(place1, track1);
        placeTrack2 = PlaceTrack.create(place2, track1);
        placeTrack3 = PlaceTrack.create(place3, track2);
        placeTrack4 = PlaceTrack.create(place4, track2);
        placeTrack5 = PlaceTrack.create(place5, track3);
        placeTrack6 = PlaceTrack.create(place6, track3);
        placeTrack7 = PlaceTrack.create(place6, track2);
        placeTrack8 = PlaceTrack.create(place6, track1);

        // 성남시 3개
        pin1 = createPin(member1, place1, placeTrack1);
        pin2 = createPin(member2, place1, placeTrack1);
        pin3 = createPin(member3, place2, placeTrack2);

        // 수원시 2개
        pin4 = createPin(member1, place3, placeTrack3);
        pin5 = createPin(member2, place3, placeTrack3);

        // 세종(의당면) 2개
        pin6 = createPin(member1, place4, placeTrack4);
        pin7 = createPin(member2, place4, placeTrack4);

        // 세종(연서면) 1개
        pin8 = createPin(member3, place5, placeTrack5);

        // 서울 성수동 32개
        pin9 = createPin(member1, place6, placeTrack6);
        pin10 = createPin(member2, place6, placeTrack7);
        pin11 = createPin(member3, place6, placeTrack8);


        memberRepository.saveAll(List.of(member1, member2, member3));
        placeRepository.saveAll(List.of(place1, place2, place3, place4, place5, place6));
        trackRepository.saveAll(List.of(track1, track2, track3));
        placeTrackRepository.saveAll(List.of(placeTrack1, placeTrack2, placeTrack3, placeTrack4, placeTrack5, placeTrack6, placeTrack7, placeTrack8));
        pinRepository.saveAll(List.of(pin1, pin2, pin3, pin4, pin5, pin6, pin7, pin8, pin9, pin10, pin11));

        placeTrack7.increaseLikeCount();
        placeTrack8.increaseLikeCount();
        placeTrack8.increaseLikeCount();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void 줌레벨이_7이하면_시도_클러스트링을_반환한다() {
        // given
        Point minPoint = geometryFactory.createPoint(
                new Coordinate(126.90, 36.50)
        );

        Point maxPoint = geometryFactory.createPoint(
                new Coordinate(127.35, 37.60)
        );

        // when
        List<PinResponse.Cluster> result = pinQueryRepository.findClusterListByViewport(minPoint, maxPoint, 6);

        // then
        assertThat(result).hasSize(3); // 경기, 세종, 서울

        PinResponse.Cluster first = result.getFirst();
        assertThat(first.clusterLevel()).isEqualTo(ClusterLevel.REGION1);
        assertThat(first.regionName()).isEqualTo("경기도");
        assertThat(first.pinCount()).isEqualTo(5);

        assertThat(first.latitude())
                .isCloseTo(37.3544, within(1e-6));
        assertThat(first.longitude())
                .isCloseTo(127.1070, within(1e-6));

        assertThat(first.bounds())
                .extracting(
                        PinResponse.Bound::southWestLat,
                        PinResponse.Bound::southWestLng,
                        PinResponse.Bound::northEastLat,
                        PinResponse.Bound::northEastLng
                )
                .containsExactly(
                        37.2865,
                        127.0615,
                        37.3947,
                        127.1483
                );
    }

    @Test
    void 줌레벨이_8이상_10이하면_시군구_클러스트링을_반환한다() {
        // given
        Point minPoint = geometryFactory.createPoint(
                new Coordinate(127.0615, 37.2865)
        );

        Point maxPoint = geometryFactory.createPoint(
                new Coordinate(127.1483, 37.3947)
        );

        // when
        List<PinResponse.Cluster> result = pinQueryRepository.findClusterListByViewport(minPoint, maxPoint, 9);

        // then
        assertThat(result).hasSize(2); // 성남, 수원

        PinResponse.Cluster first = result.getFirst();
        assertThat(first.clusterLevel()).isEqualTo(ClusterLevel.REGION2);
        assertThat(first.regionName()).isEqualTo("경기도 성남시");
        assertThat(first.pinCount()).isEqualTo(3);

        assertThat(first.latitude())
                .isCloseTo(37.38835, within(1e-6));
        assertThat(first.longitude())
                .isCloseTo(127.12975, within(1e-6));

        assertThat(first.bounds())
                .extracting(
                        PinResponse.Bound::southWestLat,
                        PinResponse.Bound::southWestLng,
                        PinResponse.Bound::northEastLat,
                        PinResponse.Bound::northEastLng
                )
                .containsExactly(
                        37.3820,
                        127.1112,
                        37.3947,
                        127.1483
                );
    }

    @Test
    void 중간리전이_null이면_하위_레벨을_반환한다() {
        // given
        Point minPoint = geometryFactory.createPoint(
                new Coordinate(127.24, 36.54)
        );

        Point maxPoint = geometryFactory.createPoint(
                new Coordinate(127.30, 36.61)
        );

        // when
        List<PinResponse.Cluster> result = pinQueryRepository.findClusterListByViewport(minPoint, maxPoint, 9);

        // then
        assertThat(result).hasSize(2); // 의당면, 안서면

        assertThat(result)
                .extracting(PinResponse.Cluster::regionName)
                .containsExactlyInAnyOrder(
                        "세종특별자치시 의당면",
                        "세종특별자치시 연서면"
                );

        PinResponse.Cluster first = result.stream()
                .filter(c -> c.regionName().equals("세종특별자치시 의당면"))
                .findFirst()
                .orElseThrow();
        assertThat(first.clusterLevel()).isEqualTo(ClusterLevel.REGION3);
        assertThat(first.pinCount()).isEqualTo(2);

        PinResponse.Cluster last = result.stream()
                .filter(c -> c.regionName().equals("세종특별자치시 연서면"))
                .findFirst()
                .orElseThrow();
        assertThat(last.clusterLevel()).isEqualTo(ClusterLevel.REGION3);
        assertThat(last.pinCount()).isEqualTo(1);
    }

    @Test
    void 줌레벨이_11이상_13이하면_읍면동_클러스트링을_반환한다() {
        // given
        Point minPoint = geometryFactory.createPoint(
                new Coordinate(127.1112, 37.3820)
        );

        Point maxPoint = geometryFactory.createPoint(
                new Coordinate(127.1483, 37.3947)
        );

        // when
        List<PinResponse.Cluster> result = pinQueryRepository.findClusterListByViewport(minPoint, maxPoint, 12);

        // then
        assertThat(result).hasSize(2); // 백현동, 서현동

        PinResponse.Cluster first = result.getFirst();
        assertThat(first.clusterLevel()).isEqualTo(ClusterLevel.REGION3);
        assertThat(first.regionName()).isEqualTo("경기도 성남시 백현동");
        assertThat(first.pinCount()).isEqualTo(2);

        assertThat(first.latitude())
                .isCloseTo(37.3947, within(1e-6));
        assertThat(first.longitude())
                .isCloseTo(127.1112, within(1e-6));

        assertThat(first.bounds())
                .extracting(
                        PinResponse.Bound::southWestLat,
                        PinResponse.Bound::southWestLng,
                        PinResponse.Bound::northEastLat,
                        PinResponse.Bound::northEastLng
                )
                .containsExactly(
                        37.3947,
                        127.1112,
                        37.3947,
                        127.1112
                );
    }

    @Test
    void 줌레벨이_14이상이면_핀목록을_반환한다() {
        // given
        Point minPoint = geometryFactory.createPoint(
                new Coordinate(127.11, 37.38)
        );

        Point maxPoint = geometryFactory.createPoint(
                new Coordinate(127.15, 37.40)
        );

        // when
        List<PinResponse.PinPreview> result = pinQueryRepository.findPinPreviewListByViewport(minPoint, maxPoint);

        // then
        assertThat(result).hasSize(2);
        PinResponse.PinPreview first = result.getFirst();
        assertThat(first.placeId()).isEqualTo(place2.getId());

        PinResponse.PinPreview last = result.getLast();
        assertThat(last.placeId()).isEqualTo(place1.getId());
    }

    @Test
    void 트랙이_다른_핀이_여러개일_시_좋아요가_가장_높은것을_반환한다() {
        // given
        Point minPoint = geometryFactory.createPoint(
                new Coordinate(127.0250, 37.5300)
        );

        Point maxPoint = geometryFactory.createPoint(
                new Coordinate(127.0750, 37.5600)
        );

        // when
        List<PinResponse.PinPreview> result = pinQueryRepository.findPinPreviewListByViewport(minPoint, maxPoint);

        // then
        assertThat(result).hasSize(1);
        PinResponse.PinPreview first = result.getFirst();
        assertThat(first.albumImageUrl()).isEqualTo("album");
    }

    @Test
    void 삭제처리된_핀은_제외하고_가장_높은것을_반환한다() {
        // given
        Point minPoint = geometryFactory.createPoint(
                new Coordinate(127.0250, 37.5300)
        );

        Point maxPoint = geometryFactory.createPoint(
                new Coordinate(127.0750, 37.5600)
        );
        // when
        List<PinResponse.PinPreview> result = pinQueryRepository.findPinPreviewListByViewport(minPoint, maxPoint);

        // then
        assertThat(result).hasSize(1);
        PinResponse.PinPreview first = result.getFirst();
        assertThat(first.albumImageUrl()).isEqualTo("album");

        // when
        Pin managedPin = pinRepository.findById(pin11.getId()).orElseThrow();

        managedPin.delete(); // 좋아요가 가장 높은 핀 삭제
        entityManager.flush();
        entityManager.clear();

        List<PinResponse.PinPreview> result2 = pinQueryRepository.findPinPreviewListByViewport(minPoint, maxPoint);

        // then
        assertThat(result2).hasSize(1);
        PinResponse.PinPreview first2 = result2.getFirst();
        assertThat(first2.albumImageUrl()).isEqualTo("album2");
    }

    private Member createMember(String name, String nickname) {
        return Member.builder()
                .name(name)
                .nickname(nickname)
                .introduction("안녕하세요")
                .profileImageObjectKey("image_url")
                .build();
    }

    private Place createPlace(String name, String address, String sido, String sigungu, String eupMyeonDong, double lng, double lat) {
        return Place.builder()
                .name(name)
                .address(address)
                .sido(sido)
                .sigungu(sigungu)
                .eupMyeonDong(eupMyeonDong)
                .source(PlaceSource.MAP_SELECTION)
                .location(geometryFactory.createPoint(new Coordinate(lng, lat)))
                .build();
    }

    private Pin createPin(Member member, Place place, PlaceTrack placeTrack) {
        return createPin(member, place, placeTrack, true);
    }

    private Pin createPin(Member member, Place place, PlaceTrack placeTrack, boolean feedPublic) {
        return Pin.builder()
                .member(member)
                .place(place)
                .placeTrack(placeTrack)
                .clipStartMs(70000)
                .introduction("good")
                .isFeedPublic(feedPublic)
                .build();
    }
}
