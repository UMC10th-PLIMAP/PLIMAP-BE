package com.example.plimap.domain.pin.service.command.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.entity.PinLike;
import com.example.plimap.domain.pin.entity.Tag;
import com.example.plimap.domain.pin.event.PinCreatedEvent;
import com.example.plimap.domain.pin.event.PinLikedEvent;
import com.example.plimap.domain.pin.exception.PinException;
import com.example.plimap.domain.pin.exception.PinLikeException;
import com.example.plimap.domain.pin.exception.TagErrorCode;
import com.example.plimap.domain.pin.exception.TagException;
import com.example.plimap.domain.pin.repository.PinLikeRepository;
import com.example.plimap.domain.pin.repository.PinRepository;
import com.example.plimap.domain.pin.repository.PinTagRepository;
import com.example.plimap.domain.pin.service.query.TagQueryService;
import com.example.plimap.domain.pin.validator.PinLocationValidator;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.Track;
import com.example.plimap.domain.track.service.command.impl.TrackCommandServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PinCommandServiceImplTest {

    @InjectMocks
    private PinCommandServiceImpl pinCommandService;

    @Mock
    TagQueryService tagQueryService;

    private final PinLocationValidator pinLocationValidator2 = new PinLocationValidator();

    @Mock
    private TrackCommandServiceImpl trackCommandService;

    @Mock
    private PinRepository pinRepository;

    @Mock
    private PinTagRepository pinTagRepository;

    @Mock
    private PlaceQueryService placeQueryService;

    @Mock
    private PinLikeRepository pinLikeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private PinLocationValidator pinLocationValidator = new PinLocationValidator();

    Member member, member2;
    Place place;
    Tag tag1, tag2, tag3, tag4, tag5;
    PlaceTrack placeTrack;
    Pin pin;
    PinLike pinLike;

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
                .displayOrder((short) 3)
                .build();

        tag2 = Tag.builder()
                .name("설렘")
                .displayOrder((short) 4)
                .build();

        tag3 = Tag.builder()
                .name("청량")
                .displayOrder((short) 9)
                .build();

        tag4 = Tag.builder()
                .name("신남")
                .displayOrder((short) 5)
                .build();

        tag5 = Tag.builder()
                .name("힙함")
                .displayOrder((short) 10)
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
                .introduction("before")
                .isFeedPublic(true)
                .build();

        pinLike = PinLike.builder()
                .member(member)
                .pin(pin)
                .build();
    }

    // createPin 테스트
    @Test
    void 핀_생성에_성공한다() {
        ReflectionTestUtils.setField(member, "id", 1L);

        PinRequest.Create request = new PinRequest.Create(
                37.5267894104045,
                127.021265055462,
                1L,
                1L,
                70000,
                "한강 야경을 보면서 듣기 좋은 분위기의 노래예요.",
                List.of("몽환", "설렘"),
                true
        );

        when(placeQueryService.getActivePlace(1L))
                .thenReturn(place);
        when(tagQueryService.getTagsByNames(anyList()))
                .thenReturn(new ArrayList<>(List.of(tag1, tag2)));
        when(trackCommandService.getOrCreatePlaceTrack(any(), any()))
                .thenReturn(placeTrack);

        PinResponse.Summary result = pinCommandService.createPin(member, request);

        assertThat(result.writerNickname())
                .isEqualTo(member.getNickname());
        verify(pinRepository).save(any(Pin.class));
        verify(pinTagRepository).saveAll(anyList());

        ArgumentCaptor<PinCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PinCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().authorId()).isEqualTo(1L);
    }

    @Test
    void 장소와_현위치가_500m_이내면_예외가_발생하지_않는다() {
        // 압구정 한강공원 <-> 신사공원
        assertThatCode(() ->
                pinLocationValidator2.validateWithin500m(
                        37.5267894104045,
                        127.021265055462,
                        place
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void 장소와_현위치_거리가_500m_이상이면_예외가_발생한다() {
        // 압구정 한강공원 <-> 서울여자대학교
        assertThatThrownBy(() -> {
            pinLocationValidator2.validateWithin500m(37.626144976334544, 127.09302024107471, place);})
                .isInstanceOf(PinException.class)
                .hasMessageContaining("사용자가 장소 반경이 500m 이상에 있어 PIN을 등록할 수 없습니다.");
    }

    @Test
    void 존재하지_않는_태그면_예외가_발생한다() {
        PinRequest.Create request = PinRequest.Create.builder()
                .userLatitude(37.5267894104045)
                .userLongitude(127.021265055462)
                .placeId(1L)
                .itunesTrackId(1L)
                .clipStartMs(70000)
                .introduction("한강 야경을 보면서 듣기 좋은 분위기의 노래예요.")
                .tags(List.of("몽환", "청량", "설렘", "신남"))
                .feedOpen(true)
                .build();

        when(placeQueryService.getActivePlace(1L))
                .thenReturn(place);
        when(tagQueryService.getTagsByNames(anyList()))
                .thenThrow(new TagException(TagErrorCode.TAG_NOT_FOUND));
        when(trackCommandService.getOrCreatePlaceTrack(any(), any()))
                .thenReturn(placeTrack);

        assertThatThrownBy(() -> pinCommandService.createPin(member, request))
                .isInstanceOf(TagException.class)
                .hasMessageContaining("태그를 찾을 수 없습니다.");
    }

    @Test
    void 태그가_5개_이상이면_예외가_발생한다() {
        PinRequest.Create request = PinRequest.Create.builder()
                .userLatitude(37.5267894104045)
                .userLongitude(127.021265055462)
                .placeId(1L)
                .itunesTrackId(1L)
                .clipStartMs(70000)
                .introduction("한강 야경을 보면서 듣기 좋은 분위기의 노래예요.")
                .tags(List.of("몽환", "청량", "설렘", "신남", "힙함"))
                .feedOpen(true)
                .build();

        when(placeQueryService.getActivePlace(1L))
                .thenReturn(place);

        assertThatThrownBy(() -> pinCommandService.createPin(member, request))
                .isInstanceOf(TagException.class)
                .hasMessageContaining("태그는 최대 4개만 등록 가능합니다.");
    }

    // updatePin 테스트
    @Test
    void 핀_수정에_성공한다() {
        Pin pin = Pin.builder()
                .member(member)
                .place(place)
                .placeTrack(placeTrack)
                .introduction("before")
                .isFeedPublic(true)
                .build();

        ReflectionTestUtils.setField(member, "id", 1L);
        ReflectionTestUtils.setField(pin, "id", 1L);

        when(pinRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(pin));

        when(tagQueryService.getTagsByNames(anyList()))
                .thenReturn(new ArrayList<>(List.of(tag2, tag3)));

        PinRequest.Update request = PinRequest.Update.builder()
                .introduction("i am all you need")
                .tags(List.of("청량", "설렘"))
                .feedOpen(false)
                .build();

        PinResponse.UpdatedPin response = pinCommandService.updatePin(member, request, 1L);

        assertThat(response.introduction()).isEqualTo(request.introduction());
        assertThat(response.tags())
                .containsExactly("설렘", "청량");
        assertThat(response.feedOpen()).isEqualTo(request.feedOpen());
    }

    // deletePin 테스트
    @Test
    void 핀_삭제에_성공한다() {
        ReflectionTestUtils.setField(member, "id", 1L);
        ReflectionTestUtils.setField(pin, "id", 1L);

        when(pinRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(pin));

        pinCommandService.deletePin(member, 1L);

        assertThat(pin.getDeletedAt()).isNotNull();
    }

    @Test
    void 작성자가_아닐시_핀_삭제에_실패한다() {
        ReflectionTestUtils.setField(member, "id", 1L);
        ReflectionTestUtils.setField(member2, "id", 2L);
        ReflectionTestUtils.setField(pin, "id", 1L);

        when(pinRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(pin));

        assertThatThrownBy(() -> pinCommandService.deletePin(member2, 1L))
                .isInstanceOf(PinException.class)
                .hasMessage("해당 PIN에 수정/삭제 권한이 없습니다.");

    }

    // 관리자 벌점 처리 테스트
    @Test
    void 관리자가_벌점을_부여하면_핀이_삭제되고_신고누적이_초기화된다() {
        ReflectionTestUtils.setField(pin, "id", 1L);
        ReflectionTestUtils.setField(pin, "reportCount", 5);

        when(pinRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(pin));

        pinCommandService.penalizePin(1L);

        assertThat(pin.getDeletedAt()).isNotNull();
        assertThat(pin.getReportCount()).isEqualTo(0);
    }

    @Test
    void 관리자가_벌점을_부여하지_않으면_신고누적만_초기화된다() {
        ReflectionTestUtils.setField(pin, "id", 1L);
        ReflectionTestUtils.setField(pin, "reportCount", 5);

        when(pinRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(pin));

        pinCommandService.resetPinReportCount(1L);

        assertThat(pin.getDeletedAt()).isNull();
        assertThat(pin.getReportCount()).isEqualTo(0);
    }

    @Test
    void 회원의_핀을_전부_하드삭제한다() {
        pinCommandService.hardDeleteAllByMember(1L);

        verify(pinRepository).deleteByMemberId(1L);
    }

    @Test
    void 핀의_신고누적을_감소시킨다() {
        pinCommandService.decreaseReportCount(1L);

        verify(pinRepository).decreaseReportCount(1L);
    }

    @Test
    void 핀의_좋아요_수를_감소시킨다() {
        pinCommandService.decreaseLikeCount(1L);

        verify(pinRepository).decreaseLikeCount(1L);
    }

    @Test
    void 회원이_누른_핀_좋아요를_전부_하드삭제한다() {
        pinCommandService.hardDeleteLikesByMember(1L);

        verify(pinLikeRepository).deleteByMemberId(1L);
    }

    // createPinLike 테스트
    @Test
    void 핀_좋아요_등록시_좋아요_개수가_증가한다() {
        ReflectionTestUtils.setField(member, "id", 1L);
        ReflectionTestUtils.setField(pin, "id", 1L);

        when(pinRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(pin));

        pinCommandService.createPinLike(member, pin.getId());

        verify(pinLikeRepository).save(any(PinLike.class));
        verify(pinRepository).increaseLikeCount(1L);

        ArgumentCaptor<PinLikedEvent> eventCaptor = ArgumentCaptor.forClass(PinLikedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().pinId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().pinOwnerId()).isEqualTo(pin.getMember().getId());
        assertThat(eventCaptor.getValue().likerId()).isEqualTo(member.getId());
    }

    @Test
    void 한사람이_같은_핀_좋아요를_여러번_요청할시_예외가_발생한다() {
        ReflectionTestUtils.setField(member, "id", 1L);
        ReflectionTestUtils.setField(pin, "id", 1L);

        when(pinRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(pin));
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(pinLikeRepository)
                .save(any(PinLike.class));

        assertThatThrownBy(() -> pinCommandService.createPinLike(member, 1L))
                .isInstanceOf(PinLikeException.class)
                .hasMessage("이미 좋아요한 핀입니다.");
    }

    // deletePinLike 테스트
    @Test
    void 핀_좋아요_삭제시_좋아요_개수가_감소한다() {
        ReflectionTestUtils.setField(member, "id", 1L);
        ReflectionTestUtils.setField(pin, "id", 1L);

        when(pinRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(pin));
        when(pinLikeRepository.findByPinAndMember(pin, member))
                .thenReturn(Optional.of(pinLike));

        pinCommandService.deletePinLike(member, pin.getId());

        verify(pinLikeRepository).delete(any(PinLike.class));
        verify(pinRepository).decreaseLikeCount(1L);
    }

    @Test
    void 좋아요하지_않은_핀을_삭제하면_예외가_발생한다() {
        ReflectionTestUtils.setField(member, "id", 1L);
        ReflectionTestUtils.setField(pin, "id", 1L);

        when(pinRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(pin));
        when(pinLikeRepository.findByPinAndMember(pin, member))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pinCommandService.deletePinLike(member, 1L))
                .isInstanceOf(PinLikeException.class)
                .hasMessage("핀 좋아요을 찾을 수 없습니다.");
    }
}
