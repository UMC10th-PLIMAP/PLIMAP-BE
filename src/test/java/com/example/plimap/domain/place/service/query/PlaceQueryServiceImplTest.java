package com.example.plimap.domain.place.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.repository.PlaceSearchHistoryRepository;
import com.example.plimap.domain.place.repository.query.PlaceQueryRepository;
import com.example.plimap.domain.place.service.query.impl.PlaceQueryServiceImpl;
import com.example.plimap.global.external.kakao.KakaoAddressSearchClient;
import com.example.plimap.global.external.kakao.KakaoClientException;
import com.example.plimap.global.external.kakao.KakaoClientTimeoutException;
import com.example.plimap.global.external.kakao.KakaoPlaceSearchClient;
import com.example.plimap.global.external.kakao.dto.KakaoAddressSearchResponse;
import com.example.plimap.global.external.kakao.dto.KakaoPlaceSearchResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceQueryServiceImplTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceSearchHistoryRepository placeSearchHistoryRepository;

    @Mock
    private PlaceQueryRepository placeQueryRepository;

    @Mock
    private KakaoAddressSearchClient kakaoAddressSearchClient;

    @Mock
    private KakaoPlaceSearchClient kakaoPlaceSearchClient;

    @Mock
    private PinQueryService pinQueryService;

    private PlaceQueryServiceImpl placeQueryService;

    @BeforeEach
    void setUp() {
        placeQueryService = new PlaceQueryServiceImpl(
                placeRepository,
                placeSearchHistoryRepository,
                placeQueryRepository,
                kakaoAddressSearchClient,
                kakaoPlaceSearchClient,
                pinQueryService
        );
        lenient().when(kakaoAddressSearchClient.search(any()))
                .thenReturn(new KakaoAddressSearchResponse(List.of()));
    }

    @Test
    void 카카오_장소와_활성_Place의_PIN_정보를_병합한다() {
        PlaceRequest.Search request = new PlaceRequest.Search(" 한강 ", 37.5283, 126.9326);
        KakaoPlaceSearchResponse response = response("26338954", "470");
        Place place = mock(Place.class);

        when(kakaoPlaceSearchClient.search("한강", 37.5283, 126.9326))
                .thenReturn(response);
        when(placeRepository.findAllByPlaceProviderAndProviderPlaceIdInAndDeletedAtIsNull(
                "KAKAO",
                List.of("26338954")
        )).thenReturn(List.of(place));
        when(place.getId()).thenReturn(12L);
        when(place.getProviderPlaceId()).thenReturn("26338954");
        when(pinQueryService.findPinInfosByPlaceIds(List.of(12L)))
                .thenReturn(Map.of(12L, new PlacePinInfo(true, "홍길동",1L)));

        PlaceResponse.SearchResult result = placeQueryService.searchPlaces(request);

        assertThat(result.items()).containsExactly(new PlaceResponse.SearchItem(
                "PLACE",
                "KAKAO",
                "26338954",
                "한강",
                "여행 > 관광,명소 > 공원",
                "서울특별시 영등포구 여의도동",
                "서울특별시 영등포구 여의동로",
                37.5283,
                126.9326,
                470,
                true,
                "홍길동"
        ));
        verify(pinQueryService).findPinInfosByPlaceIds(List.of(12L));
        verify(placeRepository, never()).save(any());
    }

    @Test
    void 주소_검색_결과가_있으면_ADDRESS로_매핑하고_키워드와_PIN_검색을_호출하지_않는다() {
        PlaceRequest.Search request = new PlaceRequest.Search(
                "서울 영등포구 여의도동 84",
                37.5251,
                126.9298
        );
        when(kakaoAddressSearchClient.search("서울 영등포구 여의도동 84"))
                .thenReturn(addressResponse(
                        "서울특별시 영등포구 여의도동 84",
                        "서울특별시 영등포구 여의동로 330",
                        "126.9326",
                        "37.5283"
                ));

        PlaceResponse.SearchResult result = placeQueryService.searchPlaces(request);

        PlaceResponse.SearchItem item = result.items().getFirst();
        assertThat(item.resultType()).isEqualTo("ADDRESS");
        assertThat(item.provider()).isEqualTo("KAKAO");
        assertThat(item.providerPlaceId()).isNull();
        assertThat(item.placeName()).isEqualTo("서울특별시 영등포구 여의동로 330");
        assertThat(item.category()).isNull();
        assertThat(item.address()).isEqualTo("서울특별시 영등포구 여의도동 84");
        assertThat(item.roadAddress()).isEqualTo("서울특별시 영등포구 여의동로 330");
        assertThat(item.latitude()).isEqualTo(37.5283);
        assertThat(item.longitude()).isEqualTo(126.9326);
        assertThat(item.distanceMeters()).isEqualTo(433);
        assertThat(item.hasPin()).isFalse();
        assertThat(item.firstPinCreatorNickname()).isNull();
        verifyNoInteractions(kakaoPlaceSearchClient, placeRepository, pinQueryService);
    }

    @Test
    void 도로명_주소가_없으면_지번_주소를_placeName으로_사용한다() {
        when(kakaoAddressSearchClient.search("여의도동 84"))
                .thenReturn(addressResponse(
                        "서울특별시 영등포구 여의도동 84",
                        null,
                        "126.9326",
                        "37.5283"
                ));

        PlaceResponse.SearchResult result = placeQueryService.searchPlaces(
                new PlaceRequest.Search("여의도동 84", 37.5283, 126.9326)
        );

        assertThat(result.items().getFirst().placeName())
                .isEqualTo("서울특별시 영등포구 여의도동 84");
        assertThat(result.items().getFirst().roadAddress()).isNull();
        assertThat(result.items().getFirst().distanceMeters()).isZero();
    }

    @Test
    void 중첩_address가_없으면_document_addressName을_사용한다() {
        KakaoAddressSearchResponse.Document document =
                new KakaoAddressSearchResponse.Document(
                        "서울특별시 영등포구 여의도동 84",
                        "126.9326",
                        "37.5283",
                        null,
                        null
                );
        when(kakaoAddressSearchClient.search("여의도동 84"))
                .thenReturn(new KakaoAddressSearchResponse(List.of(document)));

        PlaceResponse.SearchResult result = placeQueryService.searchPlaces(
                new PlaceRequest.Search("여의도동 84", 37.5283, 126.9326)
        );

        assertThat(result.items().getFirst().address())
                .isEqualTo("서울특별시 영등포구 여의도동 84");
    }

    @Test
    void 주소_검색_결과는_최대_15개만_반환한다() {
        KakaoAddressSearchResponse.Document document =
                new KakaoAddressSearchResponse.Document(
                        "서울특별시 영등포구 여의도동 84",
                        "126.9326",
                        "37.5283",
                        null,
                        null
                );
        when(kakaoAddressSearchClient.search("여의도동 84"))
                .thenReturn(new KakaoAddressSearchResponse(
                        java.util.Collections.nCopies(16, document)
                ));

        PlaceResponse.SearchResult result = placeQueryService.searchPlaces(
                new PlaceRequest.Search("여의도동 84", 37.5283, 126.9326)
        );

        assertThat(result.items()).hasSize(15);
    }

    @Test
    void 주소_결과가_없으면_기존_키워드_검색을_수행한다() {
        when(kakaoPlaceSearchClient.search("한강", 37.5283, 126.9326))
                .thenReturn(response("26338954", "470"));
        when(placeRepository.findAllByPlaceProviderAndProviderPlaceIdInAndDeletedAtIsNull(
                "KAKAO",
                List.of("26338954")
        )).thenReturn(List.of());

        PlaceResponse.SearchResult result = placeQueryService.searchPlaces(
                new PlaceRequest.Search("한강", 37.5283, 126.9326)
        );

        assertThat(result.items().getFirst().resultType()).isEqualTo("PLACE");
        verify(kakaoPlaceSearchClient).search("한강", 37.5283, 126.9326);
    }

    @Test
    void 주소와_키워드_결과가_모두_없으면_빈_items를_반환한다() {
        when(kakaoPlaceSearchClient.search("없는 장소", 37.5283, 126.9326))
                .thenReturn(new KakaoPlaceSearchResponse(List.of()));

        PlaceResponse.SearchResult result = placeQueryService.searchPlaces(
                new PlaceRequest.Search("없는 장소", 37.5283, 126.9326)
        );

        assertThat(result.items()).isEmpty();
        verifyNoInteractions(placeRepository, pinQueryService);
    }

    @Test
    void 주소_검색_API_오류는_키워드로_fallback하지_않고_502로_변환한다() {
        when(kakaoAddressSearchClient.search("여의도동 84"))
                .thenThrow(new KakaoClientException("failed"));

        assertPlaceError(
                () -> placeQueryService.searchPlaces(
                        new PlaceRequest.Search("여의도동 84", 37.5283, 126.9326)
                ),
                PlaceErrorCode.PLACE_EXTERNAL_API_ERROR
        );
        verifyNoInteractions(kakaoPlaceSearchClient);
    }

    @Test
    void 주소_검색_timeout은_키워드로_fallback하지_않고_504로_변환한다() {
        when(kakaoAddressSearchClient.search("여의도동 84"))
                .thenThrow(new KakaoClientTimeoutException("timed out", new RuntimeException()));

        assertPlaceError(
                () -> placeQueryService.searchPlaces(
                        new PlaceRequest.Search("여의도동 84", 37.5283, 126.9326)
                ),
                PlaceErrorCode.PLACE_EXTERNAL_API_TIMEOUT
        );
        verifyNoInteractions(kakaoPlaceSearchClient);
    }

    @Test
    void 주소_검색의_필수_주소가_없으면_502로_변환하고_fallback하지_않는다() {
        when(kakaoAddressSearchClient.search("여의도동 84"))
                .thenReturn(addressResponse(null, null, "126.9326", "37.5283"));

        assertPlaceError(
                () -> placeQueryService.searchPlaces(
                        new PlaceRequest.Search("여의도동 84", 37.5283, 126.9326)
                ),
                PlaceErrorCode.PLACE_EXTERNAL_API_ERROR
        );
        verifyNoInteractions(kakaoPlaceSearchClient);
    }

    @Test
    void 주소_검색의_좌표가_잘못되면_502로_변환한다() {
        when(kakaoAddressSearchClient.search("여의도동 84"))
                .thenReturn(addressResponse(
                        "서울특별시 영등포구 여의도동 84",
                        null,
                        "not-number",
                        "37.5283"
                ));

        assertPlaceError(
                () -> placeQueryService.searchPlaces(
                        new PlaceRequest.Search("여의도동 84", 37.5283, 126.9326)
                ),
                PlaceErrorCode.PLACE_EXTERNAL_API_ERROR
        );
    }

    @Test
    void 활성_provider_PLACE_SEARCH_장소를_20m_이내에서_조회한다() {
        Place expected = mock(Place.class);
        when(placeQueryRepository.findNearestActiveProviderPlaceSearchWithin(
                37.5283,
                126.9326,
                20.0
        )).thenReturn(Optional.of(expected));

        Optional<Place> result =
                placeQueryService.findNearestActiveProviderPlaceSearchWithin(
                        37.5283,
                        126.9326
                );

        assertThat(result).contains(expected);
    }

    @Test
    void 활성_Place가_없으면_PIN_없음으로_응답한다() {
        PlaceRequest.Search request = new PlaceRequest.Search("한강", 37.5283, 126.9326);
        when(kakaoPlaceSearchClient.search("한강", 37.5283, 126.9326))
                .thenReturn(response("26338954", "470"));
        when(placeRepository.findAllByPlaceProviderAndProviderPlaceIdInAndDeletedAtIsNull(
                "KAKAO",
                List.of("26338954")
        )).thenReturn(List.of());

        PlaceResponse.SearchResult result = placeQueryService.searchPlaces(request);

        assertThat(result.items().getFirst().hasPin()).isFalse();
        assertThat(result.items().getFirst().firstPinCreatorNickname()).isNull();
        verifyNoInteractions(pinQueryService);
        verify(placeRepository, never()).save(any());
    }

    @Test
    void 검색_결과가_없으면_빈_items를_반환한다() {
        PlaceRequest.Search request = new PlaceRequest.Search("없는 장소", 37.5283, 126.9326);
        when(kakaoPlaceSearchClient.search("없는 장소", 37.5283, 126.9326))
                .thenReturn(new KakaoPlaceSearchResponse(List.of()));

        PlaceResponse.SearchResult result = placeQueryService.searchPlaces(request);

        assertThat(result.items()).isEmpty();
        verifyNoInteractions(placeRepository, pinQueryService);
    }

    @Test
    void 검색어가_비어_있으면_장소_검색어_필수_예외를_던진다() {
        PlaceRequest.Search request = new PlaceRequest.Search("   ", 37.5283, 126.9326);

        assertPlaceError(
                () -> placeQueryService.searchPlaces(request),
                PlaceErrorCode.PLACE_SEARCH_KEYWORD_REQUIRED
        );
        verifyNoInteractions(kakaoPlaceSearchClient);
    }

    @Test
    void 현재_위치가_없으면_현재_위치_필수_예외를_던진다() {
        PlaceRequest.Search request = new PlaceRequest.Search("한강", null, 126.9326);

        assertPlaceError(
                () -> placeQueryService.searchPlaces(request),
                PlaceErrorCode.PLACE_CURRENT_LOCATION_REQUIRED
        );
        verifyNoInteractions(kakaoPlaceSearchClient);
    }

    @Test
    void 경도가_없으면_현재_위치_필수_예외를_던진다() {
        PlaceRequest.Search request = new PlaceRequest.Search("한강", 37.5283, null);

        assertPlaceError(
                () -> placeQueryService.searchPlaces(request),
                PlaceErrorCode.PLACE_CURRENT_LOCATION_REQUIRED
        );
        verifyNoInteractions(kakaoPlaceSearchClient);
    }

    @Test
    void 카카오_API_오류를_502_장소_예외로_변환한다() {
        PlaceRequest.Search request = new PlaceRequest.Search("한강", 37.5283, 126.9326);
        when(kakaoPlaceSearchClient.search("한강", 37.5283, 126.9326))
                .thenThrow(new KakaoClientException("failed"));

        assertPlaceError(
                () -> placeQueryService.searchPlaces(request),
                PlaceErrorCode.PLACE_EXTERNAL_API_ERROR
        );
    }

    @Test
    void 카카오_timeout을_504_장소_예외로_변환한다() {
        PlaceRequest.Search request = new PlaceRequest.Search("한강", 37.5283, 126.9326);
        when(kakaoPlaceSearchClient.search("한강", 37.5283, 126.9326))
                .thenThrow(new KakaoClientTimeoutException("timed out", new RuntimeException()));

        assertPlaceError(
                () -> placeQueryService.searchPlaces(request),
                PlaceErrorCode.PLACE_EXTERNAL_API_TIMEOUT
        );
    }

    @Test
    void 카카오_거리값이_잘못되면_502_장소_예외로_변환한다() {
        PlaceRequest.Search request = new PlaceRequest.Search("한강", 37.5283, 126.9326);
        when(kakaoPlaceSearchClient.search("한강", 37.5283, 126.9326))
                .thenReturn(response("26338954", "not-number"));

        assertPlaceError(
                () -> placeQueryService.searchPlaces(request),
                PlaceErrorCode.PLACE_EXTERNAL_API_ERROR
        );
    }

    @Test
    void 활성_장소_조회에_성공한다() {
        Long placeId = 1L;
        Place place = mock(Place.class);
        when(placeRepository.findByIdAndDeletedAtIsNull(placeId))
                .thenReturn(Optional.of(place));

        Place result = placeQueryService.getActivePlace(placeId);

        assertThat(result).isSameAs(place);
    }

    @Test
    void 활성_장소가_없으면_예외가_발생한다() {
        Long placeId = 1L;
        when(placeRepository.findByIdAndDeletedAtIsNull(placeId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> placeQueryService.getActivePlace(placeId))
                .isInstanceOfSatisfying(
                        PlaceException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PlaceErrorCode.PLACE_NOT_FOUND)
                );
    }

    @Test
    void provider와_providerPlaceId로_활성_장소를_조회한다() {
        Place place = mock(Place.class);
        when(placeRepository.findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                "KAKAO",
                "kakao-place-1"
        )).thenReturn(Optional.of(place));

        Optional<Place> result = placeQueryService.findActivePlaceByProviderAndProviderPlaceId(
                "KAKAO",
                "kakao-place-1"
        );

        assertThat(result).contains(place);
        verify(placeRepository).findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                "KAKAO",
                "kakao-place-1"
        );
    }

    private KakaoPlaceSearchResponse response(String providerPlaceId, String distance) {
        return new KakaoPlaceSearchResponse(List.of(new KakaoPlaceSearchResponse.Document(
                providerPlaceId,
                "한강",
                "여행 > 관광,명소 > 공원",
                "서울특별시 영등포구 여의도동",
                "서울특별시 영등포구 여의동로",
                "126.9326",
                "37.5283",
                distance
        )));
    }

    private KakaoAddressSearchResponse addressResponse(
            String address,
            String roadAddress,
            String longitude,
            String latitude
    ) {
        return new KakaoAddressSearchResponse(List.of(
                new KakaoAddressSearchResponse.Document(
                        address,
                        longitude,
                        latitude,
                        address == null
                                ? null
                                : new KakaoAddressSearchResponse.Address(address),
                        roadAddress == null
                                ? null
                                : new KakaoAddressSearchResponse.RoadAddress(roadAddress)
                )
        ));
    }

    private void assertPlaceError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
            PlaceErrorCode errorCode
    ) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(
                        PlaceException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode)
                );
    }
}
