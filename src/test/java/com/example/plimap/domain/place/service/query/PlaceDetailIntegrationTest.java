package com.example.plimap.domain.place.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceBookmark;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.repository.PlaceBookmarkRepository;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.repository.PlaceSearchHistoryRepository;
import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import({PostgisContainerConfiguration.class, RedisContainerConfiguration.class})
@Transactional
class PlaceDetailIntegrationTest {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private PlaceQueryService placeQueryService;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private PlaceBookmarkRepository placeBookmarkRepository;

    @Autowired
    private PlaceSearchHistoryRepository placeSearchHistoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private PinQueryService pinQueryService;

    @Test
    void 장소_상세_조회는_상태를_변경하지_않고_조회_정보를_병합한다() {
        Member member = memberRepository.save(Member.builder()
                .nickname("상세조회")
                .name("테스터")
                .build());
        Place place = placeRepository.save(place(PlaceSource.ADDRESS_SEARCH));
        placeBookmarkRepository.save(PlaceBookmark.builder()
                .place(place)
                .memberId(member.getId())
                .build());
        when(pinQueryService.findPinInfosByPlaceIds(List.of(place.getId())))
                .thenReturn(Map.of(
                        place.getId(),
                        new PlacePinInfo(true, "핀작성자", 2L)
                ));
        when(pinQueryService.existsActivePinByPlaceIdAndMemberId(
                place.getId(),
                member.getId()
        )).thenReturn(true);
        long placeCount = placeRepository.count();
        long bookmarkCount = placeBookmarkRepository.count();
        long historyCount = placeSearchHistoryRepository.count();

        PlaceResponse.Detail result = placeQueryService.getPlaceDetail(
                member.getId(),
                place.getId(),
                37.5283,
                126.9326
        );

        assertThat(result.placeId()).isEqualTo(place.getId());
        assertThat(result.hasPin()).isTrue();
        assertThat(result.firstPinCreatorNickname()).isEqualTo("핀작성자");
        assertThat(result.pinCount()).isEqualTo(2L);
        assertThat(result.bookmarkedByMe()).isTrue();
        assertThat(result.pinnedByMe()).isTrue();
        assertThat(placeRepository.count()).isEqualTo(placeCount);
        assertThat(placeBookmarkRepository.count()).isEqualTo(bookmarkCount);
        assertThat(placeSearchHistoryRepository.count()).isEqualTo(historyCount);
    }

    @Test
    void Soft_Delete된_장소는_PLACE_NOT_FOUND를_반환한다() {
        Place place = placeRepository.save(place(PlaceSource.MAP_SELECTION));
        place.delete();
        placeRepository.flush();

        assertThatThrownBy(() -> placeQueryService.getPlaceDetail(
                1L,
                place.getId(),
                37.5283,
                126.9326
        )).isInstanceOfSatisfying(
                PlaceException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(PlaceErrorCode.PLACE_NOT_FOUND)
        );
    }

    private Place place(PlaceSource source) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(126.9326, 37.5283));

        return Place.builder()
                .name("한강")
                .category(source == PlaceSource.PLACE_SEARCH ? "공원" : null)
                .address("서울특별시 영등포구 여의도동")
                .roadAddress("서울특별시 영등포구 여의동로")
                .normalizedAddress(source == PlaceSource.ADDRESS_SEARCH
                        ? "서울특별시영등포구여의도동"
                        : null)
                .placeProvider(source == PlaceSource.MAP_SELECTION ? null : "KAKAO")
                .providerPlaceId(source == PlaceSource.PLACE_SEARCH ? "place-detail" : null)
                .source(source)
                .location(location)
                .build();
    }
}
