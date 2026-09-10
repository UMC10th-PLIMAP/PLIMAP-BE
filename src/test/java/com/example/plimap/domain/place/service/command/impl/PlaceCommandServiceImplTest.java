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
import com.example.plimap.domain.place.dto.PlaceAddressDecision;
import com.example.plimap.domain.place.dto.PlaceAdministrativeRegion;
import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceBookmarkId;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.place.enums.MapSelectionStatus;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.repository.PlaceBookmarkRepository;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.repository.PlaceSearchHistoryRepository;
import com.example.plimap.domain.place.repository.lock.PlaceLockRepository;
import com.example.plimap.domain.place.repository.query.PlaceQueryRepository;
import com.example.plimap.domain.place.service.query.PlaceLocationMetadataService;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
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
    private final PlaceQueryService placeQueryService = mock(PlaceQueryService.class);
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
                placeQueryService,
                placeLocationMetadataService,
                pinQueryService
        );
        when(placeLocationMetadataService.getAdministrativeRegion(
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble()
        )).thenReturn(new PlaceAdministrativeRegion(null, null, null, null));
        when(placeLocationMetadataService.getAddressDecision(
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble()
        )).thenReturn(new PlaceAddressDecision(null, null, null));
    }

    @Test
    void 활성_장소를_북마크하면_멱등_등록하고_true를_반환한다() {
        Place place = place(1L, "한강", 37.5283, 126.9326);
        when(placeRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(place));

        PlaceResponse.BookmarkResult result = placeCommandService.bookmarkPlace(10L, 1L);

        assertThat(result).isEqualTo(new PlaceResponse.BookmarkResult(1L, true));
        verify(placeBookmarkRepository).insertIfAbsent(1L, 10L);
        verify(placeRepository, never()).save(any(Place.class));
    }

    @Test
    void 활성_장소의_내_북마크만_멱등_삭제하고_false를_반환한다() {
        Place place = place(1L, "한강", 37.5283, 126.9326);
        when(placeRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(place));

        PlaceResponse.BookmarkResult result = placeCommandService.deletePlaceBookmark(10L, 1L);

        assertThat(result).isEqualTo(new PlaceResponse.BookmarkResult(1L, false));
        verify(placeBookmarkRepository).deleteByPlaceIdAndMemberId(1L, 10L);
        verify(placeRepository, never()).save(any(Place.class));
    }

    @Test
    void 존재하지_않거나_Soft_Delete된_장소의_북마크_변경은_404를_반환한다() {
        when(placeRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> placeCommandService.bookmarkPlace(10L, 1L))
                .isInstanceOf(PlaceException.class)
                .extracting(exception -> ((PlaceException) exception).getErrorCode())
                .isEqualTo(PlaceErrorCode.PLACE_NOT_FOUND);
        assertThatThrownBy(() -> placeCommandService.deletePlaceBookmark(10L, 1L))
                .isInstanceOf(PlaceException.class)
                .extracting(exception -> ((PlaceException) exception).getErrorCode())
                .isEqualTo(PlaceErrorCode.PLACE_NOT_FOUND);

        verifyNoInteractions(placeBookmarkRepository);
    }

    @Test
    void 반경_20m_이내의_활성_PLACE_SEARCH_장소를_먼저_추천한다() {
        PlaceRequest.MapSelection request =
                request("서울특별시 영등포구 여의동로 123-4");
        Place recommendedPlace = place(
                10L,
                "카카오 판교아지트",
                PlaceSource.PLACE_SEARCH,
                37.5284,
                126.9326
        );
        when(recommendedPlace.getCategory()).thenReturn("기업");
        when(placeQueryService.findNearestActiveProviderPlaceSearchWithin(
                37.5283,
                126.9326
        )).thenReturn(Optional.of(recommendedPlace));

        PlaceResponse.MapSelectionResult result =
                placeCommandService.confirmMapSelection(request);

        assertThat(result.status()).isEqualTo(MapSelectionStatus.PLACE_SEARCH_RECOMMENDED);
        assertThat(result.mapSelection()).isNull();
        assertThat(result.recommendedPlace().placeId()).isEqualTo(10L);
        assertThat(result.recommendedPlace().placeName()).isEqualTo("카카오 판교아지트");
        assertThat(result.recommendedPlace().category()).isEqualTo("기업");
        assertThat(result.recommendedPlace().address())
                .isEqualTo("서울특별시 영등포구 여의도동");
        assertThat(result.recommendedPlace().roadAddress())
                .isEqualTo("서울특별시 영등포구 여의동로");
        assertThat(result.recommendedPlace().source()).isEqualTo(PlaceSource.PLACE_SEARCH);
        assertThat(result.recommendedPlace().latitude()).isEqualTo(37.5284);
        assertThat(result.recommendedPlace().longitude()).isEqualTo(126.9326);
        assertThat(result.recommendedPlace().distanceMeters()).isEqualTo(11);
        assertThat(result.buildingName()).isNull();
        verify(placeQueryRepository, never())
                .findNearestActiveMapSelectionWithin(37.5283, 126.9326, 20.0);
        verify(placeLockRepository, never()).acquireMapSelectionLock();
        verifyNoInteractions(placeLocationMetadataService);
        verify(placeRepository, never()).save(any(Place.class));
    }

    @Test
    void 추천_장소가_없고_건물명이_있으면_장소_검색을_요청한다() {
        PlaceRequest.MapSelection request =
                request("서울특별시 영등포구 여의동로 123-4");
        when(placeLocationMetadataService.getAddressDecision(37.5283, 126.9326))
                .thenReturn(new PlaceAddressDecision(
                        "카카오 판교아지트",
                        "지번 주소",
                        "도로명 주소"
                ));

        PlaceResponse.MapSelectionResult result =
                placeCommandService.confirmMapSelection(request);

        assertThat(result.status()).isEqualTo(MapSelectionStatus.PLACE_SEARCH_REQUIRED);
        assertThat(result.mapSelection()).isNull();
        assertThat(result.recommendedPlace()).isNull();
        assertThat(result.buildingName()).isEqualTo("카카오 판교아지트");
        verify(placeQueryRepository, never())
                .findNearestActiveMapSelectionWithin(37.5283, 126.9326, 20.0);
        verify(placeLockRepository, never()).acquireMapSelectionLock();
        verify(placeRepository, never()).save(any(Place.class));
    }

    @Test
    void 추천_장소와_건물명이_없으면_20m_이내의_MAP_SELECTION_장소를_재사용한다() {
        PlaceRequest.MapSelection request =
                request("서울특별시 영등포구 여의동로 123-4");
        Place existingPlace = place(
                1L,
                "대한민국 서울특별시 영등포구 여의동로 123-4",
                37.5283,
                126.9326
        );
        when(existingPlace.getName()).thenReturn(
                "대한민국 서울특별시 영등포구 여의동로 123-4",
                "영등포구 여의동로 123-4"
        );
        when(existingPlace.getRoadAddress())
                .thenReturn("서울특별시 영등포구 여의동로 123-4");
        when(placeRepository.save(existingPlace)).thenReturn(existingPlace);
        when(placeQueryRepository.findNearestActiveMapSelectionWithin(37.5283, 126.9326, 20.0))
                .thenReturn(Optional.of(existingPlace));

        PlaceResponse.MapSelectionResult result =
                placeCommandService.confirmMapSelection(request);

        assertThat(result.status()).isEqualTo(MapSelectionStatus.MAP_SELECTION_CONFIRMED);
        assertThat(result.mapSelection().placeId()).isEqualTo(1L);
        assertThat(result.mapSelection().placeName()).isEqualTo("영등포구 여의동로 123-4");
        assertThat(result.mapSelection().latitude()).isEqualTo(37.5283);
        assertThat(result.mapSelection().longitude()).isEqualTo(126.9326);
        assertThat(result.recommendedPlace()).isNull();
        assertThat(result.buildingName()).isNull();
        verify(placeLockRepository, never()).acquireMapSelectionLock();
        verify(existingPlace).updateMapSelectionName("영등포구 여의동로 123-4");
        verify(placeRepository).save(existingPlace);
    }

    @Test
    void 추천_장소와_건물명과_기존_장소가_없으면_MAP_SELECTION_장소를_생성한다() {
        PlaceRequest.MapSelection request =
                request("  서울특별시 영등포구 여의동로 123-4  ");
        when(placeQueryRepository.findNearestActiveMapSelectionWithin(37.5283, 126.9326, 20.0))
                .thenReturn(Optional.empty());
        when(placeLocationMetadataService.getAdministrativeRegion(37.5283, 126.9326))
                .thenReturn(administrativeRegion());
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);

        PlaceResponse.MapSelectionResult result =
                placeCommandService.confirmMapSelection(request);

        InOrder flow = inOrder(
                placeQueryService,
                placeLocationMetadataService,
                placeQueryRepository,
                placeLockRepository
        );
        flow.verify(placeQueryService)
                .findNearestActiveProviderPlaceSearchWithin(37.5283, 126.9326);
        flow.verify(placeLocationMetadataService)
                .getAddressDecision(37.5283, 126.9326);
        flow.verify(placeQueryRepository)
                .findNearestActiveMapSelectionWithin(37.5283, 126.9326, 20.0);
        flow.verify(placeLocationMetadataService)
                .getAdministrativeRegion(37.5283, 126.9326);
        flow.verify(placeLockRepository).acquireMapSelectionLock();
        verify(placeRepository).save(captor.capture());
        Place createdPlace = captor.getValue();
        assertThat(createdPlace.getName()).isEqualTo("영등포구 여의동로 123-4");
        assertThat(createdPlace.getAddress())
                .isEqualTo("서울특별시 영등포구 여의도동 123-4");
        assertThat(createdPlace.getRoadAddress())
                .isEqualTo("서울특별시 영등포구 여의동로 123-4");
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
        assertThat(result.status()).isEqualTo(MapSelectionStatus.MAP_SELECTION_CONFIRMED);
        assertThat(result.mapSelection().placeName()).isEqualTo("영등포구 여의동로 123-4");
        assertThat(result.recommendedPlace()).isNull();
        assertThat(result.buildingName()).isNull();
    }

    @Test
    void 도로명_주소가_있으면_국가명과_시도명을_제외한_장소명을_사용한다() {
        PlaceRequest.MapSelection request = request(
                "  대한민국 서울특별시 영등포구 여의동로 123-4  "
        );
        when(placeQueryRepository.findNearestActiveMapSelectionWithin(37.5283, 126.9326, 20.0))
                .thenReturn(Optional.empty());
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);

        placeCommandService.confirmMapSelection(request);

        verify(placeRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("영등포구 여의동로 123-4");
    }

    @Test
    void 도로명_주소가_없으면_지번_주소로_축약_장소명을_생성한다() {
        PlaceRequest.MapSelection request =
                new PlaceRequest.MapSelection(
                        37.5283,
                        126.9326,
                        "  대한민국 서울특별시 영등포구 여의도동 123-4  ",
                        " "
                );
        when(placeQueryRepository.findNearestActiveMapSelectionWithin(37.5283, 126.9326, 20.0))
                .thenReturn(Optional.empty());
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);

        placeCommandService.confirmMapSelection(request);

        verify(placeRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("영등포구 여의도동 123-4");
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
    void 재시도_후에도_장소를_확정하지_못하면_PLACE_NOT_FOUND를_던진다() {
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
        when(placeRepository.findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                "KAKAO",
                "26338954"
        )).thenReturn(
                Optional.of(existingPlace),
                Optional.empty(),
                Optional.empty()
        );
        when(placeLocationMetadataService.getAdministrativeRegion(37.5283, 126.9326))
                .thenReturn(null);

        assertThatThrownBy(() -> placeCommandService.selectSearchPlace(10L, request))
                .isInstanceOf(PlaceException.class)
                .extracting(exception -> ((PlaceException) exception).getErrorCode())
                .isEqualTo(PlaceErrorCode.PLACE_NOT_FOUND);

        verify(placeLockRepository, times(2))
                .acquirePlaceSelectionLock("KAKAO", "26338954");
        verify(placeRepository, never()).saveAndFlush(any(Place.class));
        verifyNoInteractions(pinQueryService);
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

    private PlaceRequest.MapSelection request(String roadAddress) {
        return new PlaceRequest.MapSelection(
                37.5283,
                126.9326,
                "  서울특별시 영등포구 여의도동 123-4  ",
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
                "PLACE",
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
