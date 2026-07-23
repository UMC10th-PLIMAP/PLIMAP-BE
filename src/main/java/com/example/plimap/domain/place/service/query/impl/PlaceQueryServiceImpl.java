package com.example.plimap.domain.place.service.query.impl;

import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
import com.example.plimap.global.external.kakao.KakaoClientException;
import com.example.plimap.global.external.kakao.KakaoClientTimeoutException;
import com.example.plimap.global.external.kakao.KakaoPlaceSearchClient;
import com.example.plimap.global.external.kakao.dto.KakaoPlaceSearchResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class PlaceQueryServiceImpl implements PlaceQueryService {

    private static final String KAKAO_PROVIDER = "KAKAO";
    private static final PlacePinInfo NO_PIN_INFO = new PlacePinInfo(false, null);

    private final PlaceRepository placeRepository;
    private final KakaoPlaceSearchClient kakaoPlaceSearchClient;
    private final PinQueryService pinQueryService;

    @Override
    public PlaceResponse.SearchResult searchPlaces(PlaceRequest.Search request) {
        validateSearchRequest(request);

        KakaoPlaceSearchResponse kakaoResponse = searchKakao(request);
        if (kakaoResponse.documents().isEmpty()) {
            return new PlaceResponse.SearchResult(List.of());
        }
        validateKakaoDocuments(kakaoResponse.documents());

        Map<String, Long> placeIdsByProviderPlaceId = findActivePlaceIds(kakaoResponse);
        Map<Long, PlacePinInfo> pinInfosByPlaceId = findPinInfos(placeIdsByProviderPlaceId);

        return new PlaceResponse.SearchResult(kakaoResponse.documents().stream()
                .map(document -> toSearchItem(
                        document,
                        placeIdsByProviderPlaceId,
                        pinInfosByPlaceId
                ))
                .toList());
    }

    @Override
    public Place getActivePlace(Long placeId) {
        return placeRepository.findByIdAndDeletedAtIsNull(placeId)
                .orElseThrow(() -> new PlaceException(PlaceErrorCode.PLACE_NOT_FOUND));
    }

    @Override
    public Optional<Place> findActivePlaceByProviderAndProviderPlaceId(
            String placeProvider,
            String providerPlaceId
    ) {
        return placeRepository.findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                placeProvider,
                providerPlaceId
        );
    }

    private void validateSearchRequest(PlaceRequest.Search request) {
        if (request.keyword() == null) {
            throw new PlaceException(PlaceErrorCode.PLACE_SEARCH_KEYWORD_REQUIRED);
        }
        if (request.latitude() == null || request.longitude() == null) {
            throw new PlaceException(PlaceErrorCode.PLACE_CURRENT_LOCATION_REQUIRED);
        }
    }

    private KakaoPlaceSearchResponse searchKakao(PlaceRequest.Search request) {
        try {
            return kakaoPlaceSearchClient.search(
                    request.keyword(),
                    request.latitude(),
                    request.longitude()
            );
        } catch (KakaoClientTimeoutException exception) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_TIMEOUT, exception);
        } catch (KakaoClientException exception) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR, exception);
        }
    }

    private Map<String, Long> findActivePlaceIds(KakaoPlaceSearchResponse response) {
        List<String> providerPlaceIds = response.documents().stream()
                .map(KakaoPlaceSearchResponse.Document::id)
                .distinct()
                .toList();

        return placeRepository
                .findAllByPlaceProviderAndProviderPlaceIdInAndDeletedAtIsNull(
                        KAKAO_PROVIDER,
                        providerPlaceIds
                )
                .stream()
                .collect(Collectors.toMap(
                        Place::getProviderPlaceId,
                        Place::getId
                ));
    }

    private void validateKakaoDocuments(List<KakaoPlaceSearchResponse.Document> documents) {
        try {
            for (KakaoPlaceSearchResponse.Document document : documents) {
                Objects.requireNonNull(document);
                Objects.requireNonNull(document.id());
                Objects.requireNonNull(document.placeName());
                Objects.requireNonNull(document.categoryName());
                Objects.requireNonNull(document.addressName());
                Double.parseDouble(Objects.requireNonNull(document.x()));
                Double.parseDouble(Objects.requireNonNull(document.y()));
                Integer.parseInt(Objects.requireNonNull(document.distance()));
            }
        } catch (RuntimeException exception) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR, exception);
        }
    }

    private Map<Long, PlacePinInfo> findPinInfos(Map<String, Long> placeIdsByProviderPlaceId) {
        List<Long> placeIds = placeIdsByProviderPlaceId.values().stream()
                .distinct()
                .toList();
        if (placeIds.isEmpty()) {
            return Map.of();
        }
        return pinQueryService.findPinInfosByPlaceIds(placeIds);
    }

    private PlaceResponse.SearchItem toSearchItem(
            KakaoPlaceSearchResponse.Document document,
            Map<String, Long> placeIdsByProviderPlaceId,
            Map<Long, PlacePinInfo> pinInfosByPlaceId
    ) {
        Long placeId = placeIdsByProviderPlaceId.get(document.id());
        PlacePinInfo pinInfo = placeId == null
                ? NO_PIN_INFO
                : pinInfosByPlaceId.getOrDefault(placeId, NO_PIN_INFO);

        return new PlaceResponse.SearchItem(
                KAKAO_PROVIDER,
                document.id(),
                document.placeName(),
                document.categoryName(),
                document.addressName(),
                document.roadAddressName(),
                Double.parseDouble(document.y()),
                Double.parseDouble(document.x()),
                Integer.parseInt(document.distance()),
                pinInfo.hasPin(),
                pinInfo.firstPinCreatorNickname()
        );
    }
}
