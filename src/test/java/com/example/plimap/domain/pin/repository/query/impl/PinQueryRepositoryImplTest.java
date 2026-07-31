package com.example.plimap.domain.pin.repository.query.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.entity.MemberFollow;
import com.example.plimap.domain.member.repository.MemberFollowRepository;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.pin.dto.Pagination;
import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.enums.PinSortType;
import com.example.plimap.domain.pin.repository.PinRepository;
import com.example.plimap.domain.pin.repository.query.PinQueryRepository;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.report.entity.Report;
import com.example.plimap.domain.report.enums.ReportCategory;
import com.example.plimap.domain.report.repository.ReportRepository;
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
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
    private MemberFollowRepository memberFollowIdRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    EntityManager entityManager;

    Pin pin1, pin2, pin3, pin4, pin5;
    private Place place1;
    private Place place2;
    private Place place3;
    private Place place4;
    Member member1, member2;
    Report report;
    PlaceTrack placeTrack1, placeTrack3;
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @BeforeEach
    void setup() {
        member1 = createMember("이서윤", "이서");
        member2 = createMember("홍길동", "동길");

        place1 = createPlace(
                "여의도 한강공원",
                "서울 영등포구 여의동로 330",
                126.9326,
                37.5283
        );

        place2 = createPlace(
                "서울숲",
                "서울 성동구 뚝섬로 273",
                127.0372,
                37.5446
        );

        place3 = createPlace(
                "남산서울타워",
                "서울 용산구 남산공원길 105",
                126.9882,
                37.5512
        );

        place4 = createPlace(
                "석촌호수",
                "서울특별시 송파구 잠실동",
                127.1025,
                37.5125
        );

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

        placeTrack1 = PlaceTrack.create(place1, track);
        PlaceTrack placeTrack2 = PlaceTrack.create(place2, track);
        placeTrack3 = PlaceTrack.create(place4, track2);

        pin1 = createPin(member1, place1, placeTrack1);
        pin2 = createPin(member2, place1, placeTrack1);
        pin3 = createPin(member2, place2, placeTrack2);
        pin4 = createPin(member2, place4, placeTrack3);
        pin5 = createPin(member1, place4, placeTrack3);

        report = Report.createPinReport(member2 ,pin5, ReportCategory.COMMERCIAL_OR_PROMOTIONAL, null);

        memberRepository.saveAll(List.of(member1, member2));
        MemberFollow memberFollow = MemberFollow.create(member1, member2);
        memberFollowIdRepository.save(memberFollow);
        placeRepository.saveAll(List.of(place1, place2, place3, place4));
        trackRepository.saveAll(List.of(track, track2));
        placeTrackRepository.saveAll(List.of(placeTrack1, placeTrack2, placeTrack3));
        pinRepository.saveAll(List.of(pin1, pin2, pin3, pin4, pin5));
        reportRepository.save(report);

        increaseLike(pin1, 3);
        increaseLike(pin2, 2);

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
        assertThat(result.get(place1.getId()).pinCount()).isEqualTo(2L);
        assertThat(result.get(place2.getId()).hasPin()).isTrue();
        assertThat(result.get(place2.getId()).firstPinCreatorNickname()).isEqualTo("동길");
        assertThat(result.get(place2.getId()).pinCount()).isEqualTo(1L);
        assertThat(result.get(place3.getId()).hasPin()).isFalse();
        assertThat(result.get(place3.getId()).firstPinCreatorNickname()).isNull();
        assertThat(result.get(place3.getId()).pinCount()).isEqualTo(0L);
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
        assertThat(result.get(place1.getId()).pinCount()).isEqualTo(1L);
    }

    @Test
    void 피드정보를_커서기반_페이지네이션으로_조회한다() {
        Pagination<PinResponse.Feed> response = pinQueryRepository.findFeedListByMemberId(member2.getId(), null, null, 2 );

        assertThat(response.data().size()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(Long.parseLong(response.nextCursor().split("/")[1])).isEqualTo(pin3.getId());
        String nextCursor = response.nextCursor();
        Pagination<PinResponse.Feed> response2 = pinQueryRepository.findFeedListByMemberId(member2.getId(), null, nextCursor, 2 );

        assertThat(response2.data().size()).isEqualTo(1);
        assertThat(response2.hasNext()).isFalse();
        assertThat(response2.nextCursor()).isNull();
    }

    @Test
    void 전달한_커서_기반으로_피드정보를_조회한다() {
        String cursor = "%s/%d".formatted(
                pin3.getCreatedAt(),
                pin3.getId()
        );
        Pagination<PinResponse.Feed> response = pinQueryRepository.findFeedListByMemberId(member2.getId(), null, cursor, 2 );
        assertThat(response.data().size()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void 내_핀_목록을_커서기반_페이지네이션으로_조회한다() {
        Pagination<PinResponse.MyPin> response = pinQueryRepository.findMyPinList(member2.getId(), null, 2 );

        assertThat(response.data().size()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(Long.parseLong(response.nextCursor().split("/")[1])).isEqualTo(pin3.getId());
        String nextCursor = response.nextCursor();
        Pagination<PinResponse.MyPin> response2 = pinQueryRepository.findMyPinList(member2.getId(), nextCursor, 2 );

        assertThat(response2.data().size()).isEqualTo(1);
        assertThat(response2.hasNext()).isFalse();
        assertThat(response2.nextCursor()).isNull();
    }

    @Test
    void 전달한_커서_기반으로_내_핀_목록을_조회한다() {
        String cursor = "%s/%d".formatted(
                pin3.getCreatedAt(),
                pin3.getId()
        );
        Pagination<PinResponse.MyPin> response = pinQueryRepository.findMyPinList(member2.getId(), cursor, 2 );
        assertThat(response.data().size()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void 팔로우한_사용자가_해당_장소에_핀을_등록했다면_true를_반환한다() {
        // given
        memberFollowIdRepository.save(
                MemberFollow.create(member1, member2)
        );

        // when
        Boolean result =
                pinQueryRepository.existsPinByMemberFollowAndPlace(
                        member1.getId(),
                        place2.getId()
                );

        // then
        assertThat(result).isTrue();
    }

    @Test
    void 팔로우한_사용자가_해당_장소에_핀을_등록하지_않았다면_false를_반환한다() {
        // given
        memberFollowIdRepository.save(
                MemberFollow.create(member1, member2)
        );

        // when
        Boolean result =
                pinQueryRepository.existsPinByMemberFollowAndPlace(
                        member1.getId(),
                        place3.getId()
                );

        // then
        assertThat(result).isFalse();
        Boolean response = pinQueryRepository.existsPinByMemberFollowAndPlace(member1.getId(), place3.getId());
        assertThat(response).isFalse();

    }

    @Test
    void 특정_장소에_대한_핀_목록을_좋아요순으로_페이지네이션으로_조회한다() {
        Pagination<PinResponse.PinDetail> response = pinQueryRepository.findPinListByPlaceTrackIdAndSortType(member2.getId(), null, 1 , PinSortType.POPULAR, placeTrack1.getId());

        assertThat(response.data().size()).isEqualTo(1);
        assertThat(response.hasNext()).isTrue();
        assertThat(Integer.parseInt(response.nextCursor().split("/")[0])).isEqualTo(3);
        assertThat(Long.parseLong(response.nextCursor().split("/")[1])).isEqualTo(pin1.getId());
        String nextCursor = response.nextCursor();

        Pagination<PinResponse.PinDetail> response2 = pinQueryRepository.findPinListByPlaceTrackIdAndSortType(member2.getId(), nextCursor, 2, PinSortType.POPULAR, placeTrack1.getId());
        assertThat(response2.data().size()).isEqualTo(1);
        assertThat(response2.hasNext()).isFalse();
        assertThat(response2.data().getFirst().pinId()).isEqualTo(pin2.getId());
        assertThat(response2.nextCursor()).isNull();
    }

    @Test
    void 특정_장소에_대한_핀_목록을_최신순으로_페이지네이션으로_조회한다() {
        Pagination<PinResponse.PinDetail> response = pinQueryRepository.findPinListByPlaceTrackIdAndSortType(member2.getId(), null, 1 , PinSortType.LATEST, placeTrack1.getId());

        assertThat(response.data().size()).isEqualTo(1);
        assertThat(response.hasNext()).isTrue();
        assertThat(Long.parseLong(response.nextCursor().split("/")[1])).isEqualTo(pin2.getId());
        String nextCursor = response.nextCursor();
        Pagination<PinResponse.PinDetail> response2 = pinQueryRepository.findPinListByPlaceTrackIdAndSortType(member2.getId(), nextCursor, 2, PinSortType.LATEST, placeTrack1.getId());

        assertThat(response2.data().size()).isEqualTo(1);
        assertThat(response2.hasNext()).isFalse();
        assertThat(response2.nextCursor()).isNull();
    }

    @Test
    void 내가_신고한_핀은_제외하고_조회한다() {
        // given
        Long reportedId = member2.getId();

        // when
        Pagination<PinResponse.PinDetail> response = pinQueryRepository.findPinListByPlaceTrackIdAndSortType(reportedId, null, 5 , null, placeTrack3.getId());

        // then
        assertThat(response.data().size()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.data())
                .extracting(PinResponse.PinDetail::pinId)
                .doesNotContain(pin5.getId())
                .containsExactly(pin4.getId());
    }

    @Test
    void 다른_회원이_신고한_핀은_포함하여_조회한다() {
        // given
        Long nonReportedId = member1.getId();

        // when
        Pagination<PinResponse.PinDetail> response = pinQueryRepository.findPinListByPlaceTrackIdAndSortType(nonReportedId, null, 5 ,  PinSortType.POPULAR, placeTrack3.getId());

        // then
        assertThat(response.data().size()).isEqualTo(2);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.data())
                .extracting(PinResponse.PinDetail::pinId)
                .containsExactly(pin5.getId(), pin4.getId());
    }

    @Test
    void 피드_조회시_내가_신고한_핀은_제외된다() {
        Pagination<PinResponse.Feed> response =
                pinQueryRepository.findFeedListByMemberId(member1.getId(), member2.getId(), null, 10);

        assertThat(response.data())
                .extracting(PinResponse.Feed::pinId)
                .doesNotContain(pin5.getId())
                .contains(pin1.getId());
    }

    @Test
    void 피드_조회시_다른_사람이_신고한_핀은_보인다() {
        Pagination<PinResponse.Feed> response =
                pinQueryRepository.findFeedListByMemberId(member1.getId(), member1.getId(), null, 10);

        assertThat(response.data())
                .extracting(PinResponse.Feed::pinId)
                .contains(pin5.getId());
    }

    @Test
    void 피드_조회시_신고누적_10회_이상인_핀은_전원에게_숨겨진다() {
        increaseReportCount(pin1, 10);
        entityManager.flush();
        entityManager.clear();

        Pagination<PinResponse.Feed> response =
                pinQueryRepository.findFeedListByMemberId(member1.getId(), null, null, 10);

        assertThat(response.data())
                .extracting(PinResponse.Feed::pinId)
                .doesNotContain(pin1.getId());
    }

    @Test
    void 내_핀_목록에서_신고누적_10회_이상인_핀은_본인에게도_숨겨진다() {
        increaseReportCount(pin1, 10);
        entityManager.flush();
        entityManager.clear();

        Pagination<PinResponse.MyPin> response =
                pinQueryRepository.findMyPinList(member1.getId(), null, 10);

        assertThat(response.data())
                .extracting(PinResponse.MyPin::pinId)
                .doesNotContain(pin1.getId());
    }

    @Test
    void 미리보기_조회시_내가_신고한_핀은_숨겨진다() {
        Optional<Pin> result = pinQueryRepository.getPinPreview(pin5.getId(), member2.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void 미리보기_조회시_다른_사람이_신고한_핀은_보인다() {
        Optional<Pin> result = pinQueryRepository.getPinPreview(pin5.getId(), member1.getId());

        assertThat(result).isPresent();
    }

    @Test
    void 미리보기_조회시_신고누적_10회_이상인_핀은_전원에게_숨겨진다() {
        increaseReportCount(pin1, 10);
        entityManager.flush();
        entityManager.clear();

        Optional<Pin> result = pinQueryRepository.getPinPreview(pin1.getId(), member1.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void 전달한_커서_기반으로_특정_장소에_대한_핀_목록을_조회한다() {
        String cursor = "%s/%d".formatted(
                pin2.getCreatedAt(),
                pin2.getId()
        );
        Pagination<PinResponse.PinDetail> response = pinQueryRepository.findPinListByPlaceTrackIdAndSortType(member2.getId(), cursor, 2, null, placeTrack1.getId());
        assertThat(response.data().size()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void 존재하는_핀이면_조회된다() {
        Optional<Pin> result = pinQueryRepository.getPinPreview(pin1.getId(), null);

        assertThat(result).isPresent();
    }

    @Test
    void 존재하지_않는_핀이면_Optional_empty를_반환한다() {
        Optional<Pin> result = pinQueryRepository.getPinPreview(999L, null);

        assertThat(result).isEmpty();
    }

    private Member createMember(String name, String nickname) {
        return Member.builder()
                .name(name)
                .nickname(nickname)
                .introduction("안녕하세요")
                .profileImageObjectKey("image_url")
                .build();
    }

    private Place createPlace(String name, String address, double lng, double lat) {
        return Place.builder()
                .name(name)
                .address(address)
                .source(PlaceSource.MAP_SELECTION)
                .location(geometryFactory.createPoint(new Coordinate(lng, lat)))
                .build();
    }

    private Pin createPin(Member member, Place place, PlaceTrack placeTrack) {
        return Pin.builder()
                .member(member)
                .place(place)
                .placeTrack(placeTrack)
                .clipStartMs(70000)
                .introduction("good")
                .isFeedPublic(true)
                .build();
    }

    private void increaseLike(Pin pin, int count) {
        for (int i = 0; i < count; i++) {
            pinRepository.increaseLikeCount(pin.getId());
        }
    }

    private void increaseReportCount(Pin pin, int count) {
        for (int i = 0; i < count; i++) {
            pinRepository.increaseReportCount(pin.getId());
        }
    }
}
