package com.example.plimap.domain.pin.service.query.impl;

import com.example.plimap.domain.member.entity.Member;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
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

    Member member;
    Place place;
    Tag tag1, tag2, tag3, tag4, tag5;
    PlaceTrack placeTrack;


    @BeforeEach
    void setup() {
        member = Member.builder()
                .name("이서윤")
                .nickname("이서")
                .introduction("안녕하세요")
                .profileImageObjectKey("image_url")
                .build();

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

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

        Track track = Track.builder()
                .previewUrl("preview")
                .albumImageUrl("album")
                .previewUrl("url_test")
                .build();

        placeTrack = PlaceTrack.builder()
                .place(place)
                .track(track)
                .build();
    }


    @Test
    void 활성_PIN을_조회한다() {
        // given
        Pin pin = Pin.builder()
                .member(member)
                .clipStartMs(0)
                .introduction("테스트 PIN")
                .isFeedPublic(true)
                .build();
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
}