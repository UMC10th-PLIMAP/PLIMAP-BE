package com.example.plimap.domain.place.service.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.dto.PlaceAdministrativeRegion;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceBookmarkId;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.repository.PlaceBookmarkRepository;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.repository.PlaceSearchHistoryRepository;
import com.example.plimap.domain.place.repository.lock.PlaceLockRepository;
import com.example.plimap.domain.place.repository.query.PlaceQueryRepository;
import com.example.plimap.domain.place.service.query.PlaceLocationMetadataService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class PlaceCommandServiceImplTest {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceBookmarkRepository placeBookmarkRepository =
            mock(PlaceBookmarkRepository.class);
    private final PlaceSearchHistoryRepository placeSearchHistoryRepository =
            mock(PlaceSearchHistoryRepository.class);
    private final PlaceQueryRepository placeQueryRepository = mock(PlaceQueryRepository.class);
    private final PlaceLockRepository placeLockRepository = mock(PlaceLockRepository.class);
    private final PlaceLocationMetadataService placeLocationMetadataService =
            mock(PlaceLocationMetadataService.class);
    private final PinQueryService pinQueryService = mock(PinQueryService.class);

    private PlaceCommandServiceImpl placeCommandService;

    @BeforeEach
    void setUp() {
        PlacePersistenceService placePersistenceService = new PlacePersistenceService(
                placeRepository,
                placeSearchHistoryRepository,
                placeQueryRepository,
                placeLockRepository
        );
        placeCommandService = new PlaceCommandServiceImpl(
                placeRepository,
                placeBookmarkRepository,
                placeSearchHistoryRepository,
                placeQueryRepository,
                placePersistenceService,
                placeLocationMetadataService,
                pinQueryService
        );
        when(placeLocationMetadataService.getAdministrativeRegion(
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble()
        )).thenReturn(new PlaceAdministrativeRegion(null, null, null, null));
    }

    @Test
    void 잠금_후_20m_이내의_기존_장소를_재사용한다() {
        PlaceRequest.MapSelection request = request("새 장소명", "도로명 주소");
        Place existingPlace = place(1L, "기존 장소", 37.5283, 126.9326);
        when(placeQueryRepository.findNearestActiveMapSelectionWithin(37.5283, 126.9326, 20.0))
                .thenReturn(Optional.of(existingPlace));

        PlaceResponse.MapSelection result = placeCommandService.confirmMapSelection(request);

        assertThat(result.placeId()).isEqualTo(1L);
        assertThat(result.placeName()).isEqualTo("기존 장소");
        assertThat(result.latitude()).isEqualTo(37.5283);
        assertThat(result.longitude()).isEqualTo(126.9326);
        verify(placeQueryRepository)
                .findNearestActiveMapSelectionWithin(37.5283, 126.9326, 20.0);
        verify(placeLockRepository, never()).acquireMapSelectionLock();
        verifyNoInteractions(placeLocationMetadataService);
        verify(placeRepository, never()).save(any(Place.class));
    }

    @Test
    void 기존_장소가_없으면_placeName을_사용해_MAP_SELECTION_장소를_생성한다() {
        PlaceRequest.MapSelection request = request("  물빛무대 앞 광장  ", "  여의동로  ");
        when(placeQueryRepository.findNearestActiveMapSelectionWithin(37.5283, 126.9326, 20.0))
                .thenReturn(Optional.empty());
        when(placeLocationMetadataService.getAdministrativeRegion(37.5283, 126.9326))
                .thenReturn(administrativeRegion());
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);

        PlaceResponse.MapSelection result = placeCommandService.confirmMapSelection(request);

        InOrder flow = inOrder(placeLocationMetadataService, placeLockRepository);
        flow.verify(placeLocationMetadataService)
                .getAdministrativeRegion(37.5283, 126.9326);
        flow.verify(placeLockRepository).acquireMapSelectionLock();
        verify(placeRepository).save(captor.capture());
        Place createdPlace = captor.getValue();
        assertThat(createdPlace.getName()).isEqualTo("물빛무대 앞 광장");
        assertThat(createdPlace.getAddress()).isEqualTo("지번 주소");
        assertThat(createdPlace.getRoadAddress()).isEqualTo("여의동로");
        assertThat(createdPlace.getSource()).isEqualTo(PlaceSource.MAP_SELECTION);
        assertThat(createdPlace.getPlaceProvider()).isNull();
        assertThat(createdPlace.getProviderPlaceId()).isNull();
        assertThat(createdPlace.getCategory()).isNull();
        assertThat(createdPlace.getAdministrativeRegionCode()).isEqualTo("1156054000");
        assertThat(createdPlace.getSido()).isEqualTo("서울특별시");
        assertThat(createdPlace.getSigungu()).isEqualTo("영등포구");
        assertThat(createdPlace.getEupMyeonDong()).isEqualTo("여의동");
        assertThat(createdPlace.getLocation().getSRID()).isEqualTo(4326);
        assertThat(createdPlace.getLocation().getX()).isEqualTo(126.9326);
        assertThat(createdPlace.getLocation().getY()).isEqualTo(37.5283);
        assertThat(result.placeName()).isEqualTo("물빛무대 앞 광장");
    }

    @Test
    void placeName이_blank이면_roadAddress를_장소명으로_사용한다() {
        PlaceRequest.MapSelection request = request("   ", "  도로명 주소  ");
        when(placeQueryRepository.findNearestActiveMapSelectionWithin(37.5283, 126.9326, 20.0))
                .thenReturn(Optional.empty());
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);

        placeCommandService.confirmMapSelection(request);

        verify(placeRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("도로명 주소");
    }

    @Test
    void placeName과_roadAddress가_없으면_address를_장소명으로_사용한다() {
        PlaceRequest.MapSelection request =
                new PlaceRequest.MapSelection(37.5283, 126.9326, null, "  지번 주소  ", " ");
        when(placeQueryRepository.findNearestActiveMapSelectionWithin(37.5283, 126.9326, 20.0))
                .thenReturn(Optional.empty());
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);

        placeCommandService.confirmMapSelection(request);

        verify(placeRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("지번 주소");
    }

    @Test
    void provider_잠금_후_활성_검색_장소를_재사용하고_PIN과_북마크를_반환한다() {
        PlaceRequest.Selection request = selectionRequest(
                "KAKAO",
                "26338954",
                37.0,
                126.0,
                37.0,
                126.0
        );
        Place existingPlace = place(
                7L,
                "기존 한강",
                PlaceSource.PLACE_SEARCH,
                37.0,
                126.0
        );
        when(placeRepository.findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                "KAKAO",
                "26338954"
        )).thenReturn(Optional.of(existingPlace));
        when(pinQueryService.findPinInfosByPlaceIds(java.util.List.of(7L)))
                .thenReturn(Map.of(7L, new PlacePinInfo(true, "홍길동", 3L)));
        when(placeBookmarkRepository.existsById(new PlaceBookmarkId(7L, 10L)))
                .thenReturn(true);

        PlaceResponse.Selection result =
                placeCommandService.selectSearchPlace(10L, request);

        assertThat(result.placeId()).isEqualTo(7L);
        assertThat(result.placeName()).isEqualTo("기존 한강");
        assertThat(result.address()).isEqualTo("서울특별시 영등포구 여의도동");
        assertThat(result.roadAddress()).isEqualTo("서울특별시 영등포구 여의동로");
        assertThat(result.source()).isEqualTo(PlaceSource.PLACE_SEARCH);
        assertThat(result.distanceMeters()).isZero();
        assertThat(result.withinAccessRange()).isTrue();
        assertThat(result.hasPin()).isTrue();
        assertThat(result.firstPinCreatorNickname()).isEqualTo("홍길동");
        assertThat(result.pinCount()).isEqualTo(3L);
        assertThat(result.bookmarkedByMe()).isTrue();
        verify(placeLockRepository).acquirePlaceSelectionLock("KAKAO", "26338954");
        verify(placeRepository, times(2))
                .findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                        "KAKAO",
                        "26338954"
                );
        verifyNoInteractions(placeLocationMetadataService);
        verify(placeLockRepository).acquirePlaceSearchHistoryLock(10L);
        verify(placeSearchHistoryRepository).upsert(10L, 7L);
        verify(placeSearchHistoryRepository).deleteExcessByMemberId(10L);
        verify(placeRepository, never()).saveAndFlush(any(Place.class));
    }

    @Test
    void 사전_조회한_장소가_잠금_전에_삭제되면_트랜잭션_밖에서_메타데이터를_조회하고_재시도한다() {
        PlaceRequest.Selection request = selectionRequest(
                "KAKAO",
                "26338954",
                37.5283,
                126.9326,
                37.5283,
                126.9326
        );
        Place existingPlace = place(
                7L,
                "삭제 예정 장소",
                PlaceSource.PLACE_SEARCH,
                37.5283,
                126.9326
        );
        Place persistedPlace = place(
                9L,
                "새 장소",
                PlaceSource.PLACE_SEARCH,
                37.5283,
                126.9326
        );
        when(placeRepository.findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                "KAKAO",
                "26338954"
        )).thenReturn(
                Optional.of(existingPlace),
                Optional.empty(),
                Optional.empty()
        );
        when(placeLocationMetadataService.getAdministrativeRegion(37.5283, 126.9326))
                .thenReturn(administrativeRegion());
        when(placeRepository.saveAndFlush(any(Place.class))).thenReturn(persistedPlace);
        when(pinQueryService.findPinInfosByPlaceIds(java.util.List.of(9L)))
                .thenReturn(Map.of());

        PlaceResponse.Selection result =
                placeCommandService.selectSearchPlace(10L, request);

        InOrder flow = inOrder(
                placeRepository,
                placeLockRepository,
                placeLocationMetadataService
        );
        flow.verify(placeRepository)
                .findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                        "KAKAO",
                        "26338954"
                );
        flow.verify(placeLockRepository)
                .acquirePlaceSelectionLock("KAKAO", "26338954");
        flow.verify(placeRepository)
                .findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                        "KAKAO",
                        "26338954"
                );
        flow.verify(placeLocationMetadataService)
                .getAdministrativeRegion(37.5283, 126.9326);
        flow.verify(placeLockRepository)
                .acquirePlaceSelectionLock("KAKAO", "26338954");
        flow.verify(placeRepository)
                .findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                        "KAKAO",
                        "26338954"
                );
        assertThat(result.placeId()).isEqualTo(9L);
        verify(placeRepository).saveAndFlush(any(Place.class));
        verify(placeSearchHistoryRepository).upsert(10L, 9L);
    }

    @Test
    void 활성_검색_장소가_없으면_PLACE_SEARCH_장소를_새로_생성한다() {
        PlaceRequest.Selection request = selectionRequest(
                " KAKAO ",
                " 26338954 ",
                37.5283,
                126.9326,
                37.5283,
                126.9326
        );
        Place persistedPlace = place(
                9L,
                "한강",
                PlaceSource.PLACE_SEARCH,
                37.5283,
                126.9326
        );
        when(placeRepository.findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                "KAKAO",
                "26338954"
        )).thenReturn(Optional.empty());
        when(placeLocationMetadataService.getAdministrativeRegion(37.5283, 126.9326))
                .thenReturn(administrativeRegion());
        when(placeRepository.saveAndFlush(any(Place.class))).thenReturn(persistedPlace);
        when(pinQueryService.findPinInfosByPlaceIds(java.util.List.of(9L)))
                .thenReturn(Map.of());
        ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);

        PlaceResponse.Selection result =
                placeCommandService.selectSearchPlace(10L, request);

        InOrder flow = inOrder(placeLocationMetadataService, placeLockRepository);
        flow.verify(placeLocationMetadataService)
                .getAdministrativeRegion(37.5283, 126.9326);
        flow.verify(placeLockRepository)
                .acquirePlaceSelectionLock("KAKAO", "26338954");
        verify(placeLockRepository).acquirePlaceSelectionLock("KAKAO", "26338954");
        verify(placeRepository, times(2))
                .findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                        "KAKAO",
                        "26338954"
                );
        verify(placeRepository).saveAndFlush(captor.capture());
        Place createdPlace = captor.getValue();
        assertThat(createdPlace.getName()).isEqualTo("한강");
        assertThat(createdPlace.getCategory()).isEqualTo("공원");
        assertThat(createdPlace.getAddress()).isEqualTo("서울특별시 영등포구 여의도동");
        assertThat(createdPlace.getRoadAddress()).isEqualTo("서울특별시 영등포구 여의동로");
        assertThat(createdPlace.getAdministrativeRegionCode()).isEqualTo("1156054000");
        assertThat(createdPlace.getSido()).isEqualTo("서울특별시");
        assertThat(createdPlace.getSigungu()).isEqualTo("영등포구");
        assertThat(createdPlace.getEupMyeonDong()).isEqualTo("여의동");
        assertThat(createdPlace.getPlaceProvider()).isEqualTo("KAKAO");
        assertThat(createdPlace.getProviderPlaceId()).isEqualTo("26338954");
        assertThat(createdPlace.getSource()).isEqualTo(PlaceSource.PLACE_SEARCH);
        assertThat(createdPlace.getLocation().getX()).isEqualTo(126.9326);
        assertThat(createdPlace.getLocation().getY()).isEqualTo(37.5283);
        assertThat(result.placeId()).isEqualTo(9L);
        assertThat(result.address()).isEqualTo("서울특별시 영등포구 여의도동");
        assertThat(result.roadAddress()).isEqualTo("서울특별시 영등포구 여의동로");
        assertThat(result.hasPin()).isFalse();
        assertThat(result.firstPinCreatorNickname()).isNull();
        assertThat(result.pinCount()).isZero();
        assertThat(result.bookmarkedByMe()).isFalse();
    }

    @Test
    void 오백미터를_초과해도_선택에_성공하고_접근_범위를_false로_반환한다() {
        PlaceRequest.Selection request = selectionRequest(
                "KAKAO",
                "far-place",
                0.009,
                0.0,
                0.0,
                0.0
        );
        Place existingPlace = place(
                11L,
                "먼 장소",
                PlaceSource.PLACE_SEARCH,
                0.009,
                0.0
        );
        when(placeRepository.findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                "KAKAO",
                "far-place"
        )).thenReturn(Optional.of(existingPlace));
        when(pinQueryService.findPinInfosByPlaceIds(java.util.List.of(11L)))
                .thenReturn(Map.of());

        PlaceResponse.Selection result =
                placeCommandService.selectSearchPlace(10L, request);

        assertThat(result.distanceMeters()).isGreaterThan(500);
        assertThat(result.withinAccessRange()).isFalse();
    }

    @Test
    void 필수_장소_정보가_올바르지_않으면_명세_예외를_던진다() {
        PlaceRequest.Selection request = selectionRequest(
                "NAVER",
                "26338954",
                37.5283,
                126.9326,
                37.5251,
                126.9298
        );

        assertThatThrownBy(() -> placeCommandService.selectSearchPlace(10L, request))
                .isInstanceOf(PlaceException.class)
                .extracting(exception -> ((PlaceException) exception).getErrorCode())
                .isEqualTo(PlaceErrorCode.PLACE_SELECTION_INVALID);

        verifyNoInteractions(
                placeRepository,
                placeLockRepository,
                pinQueryService,
                placeBookmarkRepository,
                placeSearchHistoryRepository
        );
    }

    private PlaceRequest.MapSelection request(String placeName, String roadAddress) {
        return new PlaceRequest.MapSelection(
                37.5283,
                126.9326,
                placeName,
                "  지번 주소  ",
                roadAddress
        );
    }

    private PlaceAdministrativeRegion administrativeRegion() {
        return new PlaceAdministrativeRegion(
                "1156054000",
                "서울특별시",
                "영등포구",
                "여의동"
        );
    }

    private PlaceRequest.Selection selectionRequest(
            String provider,
            String providerPlaceId,
            double latitude,
            double longitude,
            double userLatitude,
            double userLongitude
    ) {
        return new PlaceRequest.Selection(
                provider,
                providerPlaceId,
                "  한강  ",
                "  공원  ",
                "  서울특별시 영등포구 여의도동  ",
                "  서울특별시 영등포구 여의동로  ",
                latitude,
                longitude,
                userLatitude,
                userLongitude
        );
    }

    private Place place(Long id, String name, double latitude, double longitude) {
        return place(id, name, PlaceSource.MAP_SELECTION, latitude, longitude);
    }

    private Place place(
            Long id,
            String name,
            PlaceSource source,
            double latitude,
            double longitude
    ) {
        Place place = mock(Place.class);
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        when(place.getId()).thenReturn(id);
        when(place.getName()).thenReturn(name);
        when(place.getAddress()).thenReturn("서울특별시 영등포구 여의도동");
        when(place.getRoadAddress()).thenReturn("서울특별시 영등포구 여의동로");
        when(place.getSource()).thenReturn(source);
        when(place.getLocation()).thenReturn(location);
        return place;
    }
}
