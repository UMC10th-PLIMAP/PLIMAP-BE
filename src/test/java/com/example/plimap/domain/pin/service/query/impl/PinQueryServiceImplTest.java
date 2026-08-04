package com.example.plimap.domain.pin.service.query.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.converter.PinConverter;
import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.entity.Tag;
import com.example.plimap.domain.pin.enums.AvailabilityStatus;
import com.example.plimap.domain.pin.exception.PinErrorCode;
import com.example.plimap.domain.pin.exception.PinException;
import com.example.plimap.domain.pin.repository.PinRepository;
import com.example.plimap.domain.pin.repository.query.PinQueryRepository;
import com.example.plimap.domain.pin.validator.PinLocationValidator;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.track.dto.AlbumImage;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PinQueryServiceImplTest {

    @InjectMocks
    private PinQueryServiceImpl pinQueryService;

    @Mock
    private PinQueryRepository pinQueryRepository;

    @Mock
    private PinRepository pinRepository;

    @Spy
    private PinLocationValidator pinLocationValidator = new PinLocationValidator();

    Member member, member2;
    Place place;
    Tag tag1, tag2, tag3, tag4, tag5;
    PlaceTrack placeTrack;
    Pin pin;

    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @BeforeEach
    void setup() {
        member = Member.builder()
                .name("이서윤")
                .nickname("이서")
                .introduction("안녕하세요")
                .profileImageObjectKey("image_url")
                .build();

        member2 = Member.builder()
                .name("홍길동")
                .nickname("동길")
                .introduction("안녕하세요")
                .profileImageObjectKey("image_url")
                .build();

        Point point = geometryFactory.createPoint(
                new Coordinate(127.020001338463, 37.5265858245219)
        );
        place = Place.builder()
                .name("한강공원")
                .address("서울 강남구 압구정동 387")
                .source(PlaceSource.MAP_SELECTION)
                .location(point)
                .build();

        tag1 = Tag.builder()
                .name("몽환")
                .displayOrder((short) 1)
                .build();

        tag2 = Tag.builder()
                .name("설렘")
                .displayOrder((short) 2)
                .build();

        tag3 = Tag.builder()
                .name("청량")
                .displayOrder((short) 2)
                .build();

        tag4 = Tag.builder()
                .name("신남")
                .displayOrder((short) 2)
                .build();

        tag5 = Tag.builder()
                .name("힙함")
                .displayOrder((short) 2)
                .build();

        Track track = Track.create(
                null,
                null,
                null,
                null,
                null,
                "album",
                "url_test",
                null
        );

        placeTrack = PlaceTrack.create(place, track);

        pin = Pin.builder()
                .member(member)
                .place(place)
                .placeTrack(placeTrack)
                .clipStartMs(0)
                .introduction("테스트 PIN")
                .isFeedPublic(true)
                .build();

        ReflectionTestUtils.setField(member, "id", 1L);
        ReflectionTestUtils.setField(member2, "id", 2L);
        ReflectionTestUtils.setField(place, "id", 1L);
        ReflectionTestUtils.setField(pin, "id", 1L);
    }


    @Test
    void 활성_PIN을_조회한다() {
        // given
        when(pinRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(pin));

        // when
        Pin result = pinQueryService.getActivePin(1L);

        // then
        assertThat(result).isSameAs(pin);
    }

    @Test
    void 존재하지_않거나_삭제된_PIN은_조회할_수_없다() {
        // given
        when(pinRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pinQueryService.getActivePin(1L))
                .isInstanceOfSatisfying(PinException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PinErrorCode.PIN_NOT_FOUND));
    }

    // validatePinAvailability 테스트
    @Test
    void 지도_선택위치_검증_등록가능시_CREATABLE_NEW_PLACE_상태를_반환한다() {
        PinRequest.PinAvailability request = PinRequest.PinAvailability.builder()
                .latitude(37.629000)
                .longitude(127.094000)
                .userLatitude(37.626144976334544)
                .userLongitude(127.0930202410747)
                .build();
        when(pinLocationValidator.calculateDistance(request.userLatitude(), request.userLongitude(), request.latitude(), request.longitude()))
                .thenReturn(328.98070823323764);

        PinResponse.PinAvailability result = pinQueryService.validatePinAvailability(request);
        assertThat(result.status())
                .isEqualTo(AvailabilityStatus.CREATABLE_NEW_PLACE);
        assertThat(result.registrable()).isTrue();
        assertThat(result.nearestPinDistanceMeters()).isNull();
    }

    @Test
    void 지도_선택위치_검증_500m_초과시_OUT_OF_RANGE_상태를_반환한다() {
        PinRequest.PinAvailability request = PinRequest.PinAvailability.builder()
                .latitude(37.5283)
                .longitude(126.9326)
                .userLatitude(37.626144976334544)
                .userLongitude(127.09302024107471)
                .build();
        when(pinLocationValidator.calculateDistance(request.userLatitude(), request.userLongitude(), request.latitude(), request.longitude()))
                .thenReturn(17838.988483971672);

        PinResponse.PinAvailability result = pinQueryService.validatePinAvailability(request);
        assertThat(result.status())
                .isEqualTo(AvailabilityStatus.OUT_OF_RANGE);
        assertThat(result.registrable()).isFalse();
        assertThat(result.nearestPinDistanceMeters()).isNull();

        verify(pinQueryRepository, never())
                .findNearestActivePinWithin20m(anyDouble(), anyDouble());
    }

    @Test
    void 지도_선택위치_검증_20m_이내_핀_존재시_TOO_CLOSE_TO_PIN_상태를_반환한다() {
        PinRequest.PinAvailability request = PinRequest.PinAvailability.builder()
                .latitude(37.5282)
                .longitude(126.9326)
                .userLatitude(37.5278)
                .userLongitude(126.9319)
                .build();
        when(pinLocationValidator.calculateDistance(request.userLatitude(), request.userLongitude(), request.latitude(), request.longitude()))
                .thenReturn(76.0836069534716);

        when(pinQueryRepository.findNearestActivePinWithin20m(request.latitude(), request.longitude()))
                .thenReturn(Optional.of(11.09875689));

        PinResponse.PinAvailability result = pinQueryService.validatePinAvailability(request);
        assertThat(result.status())
                .isEqualTo(AvailabilityStatus.TOO_CLOSE_TO_PIN);
        assertThat(result.registrable()).isFalse();
        assertThat(result.nearestPinDistanceMeters()).isNotNull();
    }

    @Test
    void 빈_장소_아이디목록_입력시_빈_Map을_반환한다() {
        Map<Long, PlacePinInfo> result = pinQueryService.findPinInfosByPlaceIds(List.of());
        assertThat(result).isEqualTo(Collections.emptyMap());
    }

    @Test
    void 내가_핀을_등록했다면_true를_반환한다() {
        // given
        when(pinRepository.existsByMemberAndPlaceAndDeletedAtIsNull(member, place))
                .thenReturn(true);

        // when
        boolean result = pinQueryService.validatePlacePinAccessByMember(member, place);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void 팔로우한_사람이_핀을_등록했다면_true를_반환한다() {
        // given
        when(pinRepository.existsByMemberAndPlaceAndDeletedAtIsNull(member, place))
                .thenReturn(false);
        when(pinQueryRepository.existsPinByMemberFollowAndPlace(anyLong(), anyLong()))
                .thenReturn(true);

        // when
        boolean result = pinQueryService.validatePlacePinAccessByMember(member, place);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void 둘_다_핀을_등록하지_않았다면_false를_반환한다() {
        // given
        when(pinRepository.existsByMemberAndPlaceAndDeletedAtIsNull(member, place))
                .thenReturn(false);
        when(pinQueryRepository.existsPinByMemberFollowAndPlace(anyLong(), anyLong()))
                .thenReturn(false);

        // when
        boolean result = pinQueryService.validatePlacePinAccessByMember(member, place);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void 핀_상세조회에_성공한다() {
        // given
        when(pinQueryRepository.getPinPreview(anyLong()))
                .thenReturn(Optional.ofNullable(pin));

        // when
        PinResponse.PinPreview result = pinQueryService.getPinPreview(pin.getId());

        // then
        assertThat(result.introduction()).isEqualTo(pin.getIntroduction());
        assertThat(result.clipStartMs()).isEqualTo(pin.getClipStartMs());
        assertThat(result.writerNickname()).isEqualTo(pin.getMember().getNickname());
        assertThat(result.writerProfileImage()).isEqualTo(pin.getMember().getProfileImageObjectKey());
        assertThat(result.placeId()).isEqualTo(pin.getPlace().getId());
        assertThat(result.latitude()).isEqualTo(pin.getPlace().getLocation().getY());
        assertThat(result.longitude()).isEqualTo(pin.getPlace().getLocation().getX());
        assertThat(result.albumImageUrl()).isEqualTo(pin.getPlaceTrack().getTrack().getAlbumImageUrl());
    }

    @Test
    void 줌레벨이_13이하면_클러스터를_조회한다() {
        // given
        PinRequest.Viewport request = new PinRequest.Viewport(
                36.50, 126.90, 37.60, 127.35, 7
        );

        List<PinResponse.Cluster> previews = List.of(mock(PinResponse.Cluster.class));

        given(pinQueryRepository.findClusterListByViewport(any(), any(), anyInt()))
                .willReturn(previews);

        // when
        PinResponse.ClusterAndPin result = pinQueryService.getClusterPinList(request);

        // then
        verify(pinQueryRepository, never()).findPinPreviewListByViewport(any(), any());
        verify(pinQueryRepository).findClusterListByViewport(any(), any(), anyInt());

        assertThat(result.pins()).isNull();
        assertThat(result.clusters()).hasSize(1);
    }

    @Test
    void 줌레벨이_14이상이면_핀목록을_조회한다() {
        // given
        PinRequest.Viewport request = new PinRequest.Viewport(
                37.38,127.11, 37.40, 127.15, 14
        );

        List<PinResponse.PinPreview> previews = List.of(mock(PinResponse.PinPreview.class));

        given(pinQueryRepository.findPinPreviewListByViewport(any(), any()))
                .willReturn(previews);

        // when
        PinResponse.ClusterAndPin result = pinQueryService.getClusterPinList(request);

        // then
        verify(pinQueryRepository).findPinPreviewListByViewport(any(), any());
        verify(pinQueryRepository, never())
                .findClusterListByViewport(any(), any(), anyInt());

        assertThat(result.pins()).hasSize(1);
        assertThat(result.clusters()).isNull();
    }

    @Test
    void 장소와_사용자_ID로_활성_핀_존재_여부를_조회한다() {
        // given
        Long placeId = 1L;
        Long memberId = 2L;

        when(pinQueryRepository.existsActivePinByPlaceIdAndMemberId(placeId, memberId))
                .thenReturn(true);

        // when
        boolean result = pinQueryService.existsActivePinByPlaceIdAndMemberId(placeId, memberId);

        // then
        assertThat(result).isTrue();
        verify(pinQueryRepository)
                .existsActivePinByPlaceIdAndMemberId(placeId, memberId);
        verifyNoMoreInteractions(pinQueryRepository);
    }

    @Test
    void 장소_아이디_목록으로_대표_pinTrack_이미지를_조회한다() {
        // given
        List<Long> placeIds = new ArrayList<>(List.of(1L,2L));
        when(pinQueryRepository.findRepresentativePlaceTracksByPlaceIds(placeIds))
                .thenReturn(
                        List.of(
                                AlbumImage.builder().albumImageUrl("url1").build(),
                                AlbumImage.builder().albumImageUrl("url2").build()
                        )
                );

        // when
        List<AlbumImage> result = pinQueryRepository.findRepresentativePlaceTracksByPlaceIds(placeIds);

        // then
        assertThat(result.getFirst().albumImageUrl()).isEqualTo("url1");
        assertThat(result.getLast().albumImageUrl()).isEqualTo("url2");
        verify(pinQueryRepository).findRepresentativePlaceTracksByPlaceIds(placeIds);
    }
}
