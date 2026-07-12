package com.example.plimap.domain.place.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.support.PostgisContainerConfiguration;
import jakarta.persistence.EntityManager;
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

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
@Transactional
class PlaceRepositoryIntegrationTest {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 카테고리와_위치를_저장하고_provider로_조회한다() {
        Place savedPlace = placeRepository.saveAndFlush(createSearchPlace("kakao-place-1"));
        entityManager.clear();

        Place foundPlace = placeRepository
                .findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                        "KAKAO",
                        "kakao-place-1"
                )
                .orElseThrow();

        assertThat(foundPlace.getId()).isEqualTo(savedPlace.getId());
        assertThat(foundPlace.getCategory()).isEqualTo("음식점 > 카페");
        assertThat(foundPlace.getLocation().getSRID()).isEqualTo(4326);
        assertThat(foundPlace.getLocation().getX()).isEqualTo(127.1234);
        assertThat(foundPlace.getLocation().getY()).isEqualTo(37.5678);
    }

    @Test
    void 삭제된_장소는_id와_provider로_조회되지_않는다() {
        Place savedPlace = placeRepository.saveAndFlush(createSearchPlace("kakao-place-2"));
        savedPlace.delete();
        placeRepository.flush();
        entityManager.clear();

        var foundById = placeRepository.findByIdAndDeletedAtIsNull(savedPlace.getId());
        var foundByProvider = placeRepository
                .findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                        "KAKAO",
                        "kakao-place-2"
                );

        assertThat(foundById).isEmpty();
        assertThat(foundByProvider).isEmpty();
    }

    @Test
    void category가_없는_지도_선택_장소를_저장한다() {
        Place savedPlace = placeRepository.saveAndFlush(createMapSelectionPlace());
        entityManager.clear();

        Place foundPlace = placeRepository.findByIdAndDeletedAtIsNull(savedPlace.getId())
                .orElseThrow();

        assertThat(foundPlace.getCategory()).isNull();
        assertThat(foundPlace.getPlaceProvider()).isNull();
        assertThat(foundPlace.getProviderPlaceId()).isNull();
        assertThat(foundPlace.getSource()).isEqualTo(PlaceSource.MAP_SELECTION);
    }

    private Place createSearchPlace(String providerPlaceId) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(127.1234, 37.5678));

        return Place.builder()
                .name("테스트 장소")
                .category("음식점 > 카페")
                .address("서울특별시 테스트구")
                .roadAddress("서울특별시 테스트구 테스트로 1")
                .placeProvider("KAKAO")
                .providerPlaceId(providerPlaceId)
                .source(PlaceSource.PLACE_SEARCH)
                .location(location)
                .build();
    }

    private Place createMapSelectionPlace() {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(127.4321, 37.8765));

        return Place.builder()
                .name("지도 선택 장소")
                .address("서울특별시 테스트구")
                .source(PlaceSource.MAP_SELECTION)
                .location(location)
                .build();
    }
}
