package com.example.plimap.domain.place.service.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceBookmarkId;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.repository.PlaceBookmarkRepository;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.service.command.PlaceCommandService;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import jakarta.persistence.EntityManager;
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
class PlaceBookmarkIntegrationTest {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private PlaceCommandService placeCommandService;

    @Autowired
    private PlaceQueryService placeQueryService;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private PlaceBookmarkRepository placeBookmarkRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private PinQueryService pinQueryService;

    @Test
    void 최초와_중복_등록은_한_행만_유지하고_상세에_true를_반영한다() {
        Member member = saveMember("북마크회원");
        Place place = placeRepository.save(place("북마크장소"));
        when(pinQueryService.findPinInfosByPlaceIds(java.util.List.of(place.getId())))
                .thenReturn(Map.of());

        PlaceResponse.BookmarkResult first = placeCommandService.bookmarkPlace(
                member.getId(),
                place.getId()
        );
        PlaceResponse.BookmarkResult duplicate = placeCommandService.bookmarkPlace(
                member.getId(),
                place.getId()
        );
        PlaceResponse.Detail detail = placeQueryService.getPlaceDetail(
                member.getId(),
                place.getId(),
                37.5283,
                126.9326
        );

        assertThat(first.bookmarkedByMe()).isTrue();
        assertThat(duplicate.bookmarkedByMe()).isTrue();
        assertThat(placeBookmarkRepository.findAllById(java.util.List.of(
                new PlaceBookmarkId(place.getId(), member.getId())
        ))).hasSize(1);
        assertThat(detail.bookmarkedByMe()).isTrue();
    }

    @Test
    void 삭제와_미등록_삭제는_성공하고_상세에_false를_반영한다() {
        Member member = saveMember("삭제회원");
        Place place = placeRepository.save(place("삭제장소"));
        when(pinQueryService.findPinInfosByPlaceIds(java.util.List.of(place.getId())))
                .thenReturn(Map.of());
        placeCommandService.bookmarkPlace(member.getId(), place.getId());

        PlaceResponse.BookmarkResult deleted = placeCommandService.deletePlaceBookmark(
                member.getId(),
                place.getId()
        );
        PlaceResponse.BookmarkResult missing = placeCommandService.deletePlaceBookmark(
                member.getId(),
                place.getId()
        );
        PlaceResponse.Detail detail = placeQueryService.getPlaceDetail(
                member.getId(),
                place.getId(),
                37.5283,
                126.9326
        );

        assertThat(deleted.bookmarkedByMe()).isFalse();
        assertThat(missing.bookmarkedByMe()).isFalse();
        assertThat(placeBookmarkRepository.existsById(
                new PlaceBookmarkId(place.getId(), member.getId())
        )).isFalse();
        assertThat(detail.bookmarkedByMe()).isFalse();
    }

    @Test
    void 사용자별_북마크는_격리되고_장소는_변경되지_않는다() {
        Member firstMember = saveMember("첫회원");
        Member secondMember = saveMember("둘회원");
        Place place = placeRepository.save(place("격리장소"));
        placeRepository.flush();
        entityManager.clear();
        Place persistedPlace = placeRepository.findById(place.getId()).orElseThrow();
        String originalName = persistedPlace.getName();
        var originalUpdatedAt = persistedPlace.getUpdatedAt();

        placeCommandService.bookmarkPlace(firstMember.getId(), persistedPlace.getId());
        placeCommandService.bookmarkPlace(secondMember.getId(), persistedPlace.getId());

        placeCommandService.deletePlaceBookmark(firstMember.getId(), persistedPlace.getId());
        placeRepository.flush();

        assertThat(placeBookmarkRepository.existsById(
                new PlaceBookmarkId(persistedPlace.getId(), firstMember.getId())
        )).isFalse();
        assertThat(placeBookmarkRepository.existsById(
                new PlaceBookmarkId(persistedPlace.getId(), secondMember.getId())
        )).isTrue();
        entityManager.clear();
        Place unchanged = placeRepository.findById(persistedPlace.getId()).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo(originalName);
        assertThat(unchanged.getUpdatedAt()).isEqualTo(originalUpdatedAt);
        assertThat(unchanged.getDeletedAt()).isNull();
    }

    @Test
    void 존재하지_않거나_Soft_Delete된_장소는_등록과_삭제에서_404를_반환한다() {
        Member member = saveMember("예외회원");
        Place deletedPlace = placeRepository.save(place("삭제된장소"));
        deletedPlace.delete();
        placeRepository.flush();

        assertPlaceNotFound(() -> placeCommandService.bookmarkPlace(
                member.getId(),
                Long.MAX_VALUE
        ));
        assertPlaceNotFound(() -> placeCommandService.deletePlaceBookmark(
                member.getId(),
                Long.MAX_VALUE
        ));
        assertPlaceNotFound(() -> placeCommandService.bookmarkPlace(
                member.getId(),
                deletedPlace.getId()
        ));
        assertPlaceNotFound(() -> placeCommandService.deletePlaceBookmark(
                member.getId(),
                deletedPlace.getId()
        ));
    }

    private void assertPlaceNotFound(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(PlaceException.class)
                .extracting(exception -> ((PlaceException) exception).getErrorCode())
                .isEqualTo(PlaceErrorCode.PLACE_NOT_FOUND);
    }

    private Member saveMember(String nickname) {
        return memberRepository.save(Member.builder()
                .nickname(nickname)
                .name("테스터")
                .build());
    }

    private Place place(String name) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(126.9326, 37.5283));
        return Place.builder()
                .name(name)
                .address("서울특별시 영등포구 여의도동")
                .roadAddress("서울특별시 영등포구 여의동로")
                .source(PlaceSource.MAP_SELECTION)
                .location(location)
                .build();
    }
}
