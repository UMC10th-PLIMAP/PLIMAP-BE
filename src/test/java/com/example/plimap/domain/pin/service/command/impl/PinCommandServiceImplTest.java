package com.example.plimap.domain.pin.service.command.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.entity.PinTag;
import com.example.plimap.domain.pin.entity.Tag;
import com.example.plimap.domain.pin.exception.PinException;
import com.example.plimap.domain.pin.repository.PinRepository;
import com.example.plimap.domain.pin.repository.PinTagRepository;
import com.example.plimap.domain.pin.repository.TagRepository;
import com.example.plimap.domain.pin.validator.PinLocationValidator;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.Track;
import com.example.plimap.domain.track.repository.PlaceTrackRepository;
import com.example.plimap.domain.track.repository.TrackRepository;
import com.example.plimap.domain.track.service.command.impl.TrackCommandServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PinCommandServiceImplTest {

    @InjectMocks
    private PinCommandServiceImpl pinCommandService;

    private final PinLocationValidator pinLocationValidator2 = new PinLocationValidator();

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private TrackCommandServiceImpl trackCommandService;

    @Mock
    private PinRepository pinRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private PinTagRepository pinTagRepository;

    Member member;
    Place place;
    Tag tag1, tag2;
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
    void 핀_생성에_성공한다() {
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

        when(placeRepository.findById(1L))
                .thenReturn(Optional.of(place));
        when(tagRepository.findAllByNameIn(anyList()))
                .thenReturn(new ArrayList<>(List.of(tag1, tag2)));
        when(trackCommandService.getOrCreatePlaceTrack(any(), any()))
                .thenReturn(placeTrack);

        PinResponse.Summary result = pinCommandService.createPin(member, request);

        assertThat(result.writerNickname())
                .isEqualTo(member.getNickname());
        verify(pinRepository).save(any(Pin.class));
        verify(pinTagRepository).saveAll(anyList());
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

}