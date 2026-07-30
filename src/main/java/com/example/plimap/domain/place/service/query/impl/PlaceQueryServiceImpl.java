package com.example.plimap.domain.place.service.query.impl;

import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceSearchHistory;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.repository.PlaceSearchHistoryRepository;
import com.example.plimap.domain.place.repository.query.PlaceQueryRepository;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
import com.example.plimap.global.external.kakao.KakaoAddressSearchClient;
import com.example.plimap.global.external.kakao.KakaoClientException;
import com.example.plimap.global.external.kakao.KakaoClientTimeoutException;
import com.example.plimap.global.external.kakao.KakaoPlaceSearchClient;
import com.example.plimap.global.external.kakao.dto.KakaoAddressSearchResponse;
import com.example.plimap.global.external.kakao.dto.KakaoPlaceSearchResponse;
import com.example.plimap.global.util.GeoDistanceCalculator;
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
    private static final String ADDRESS_RESULT_TYPE = "ADDRESS";
    private static final String PLACE_RESULT_TYPE = "PLACE";
    private static final int MAX_SEARCH_RESULTS = 15;
    private static final double PROVIDER_PLACE_SEARCH_DISTANCE_METERS = 20.0;
    private static final PlacePinInfo NO_PIN_INFO = new PlacePinInfo(false, null, 0L);

    private final PlaceRepository placeRepository;
    private final PlaceSearchHistoryRepository placeSearchHistoryRepository;
    private final PlaceQueryRepository placeQueryRepository;
    private final KakaoAddressSearchClient kakaoAddressSearchClient;
    private final KakaoPlaceSearchClient kakaoPlaceSearchClient;
    private final PinQueryService pinQueryService;

    @Override
    public PlaceResponse.SearchResult searchPlaces(PlaceRequest.Search request) {
        validateSearchRequest(request);

        KakaoAddressSearchResponse addressResponse = searchKakaoAddress(request.keyword());
        if (!addressResponse.documents().isEmpty()) {
            return toAddressSearchResult(addressResponse, request);
        }

        KakaoPlaceSearchResponse kakaoResponse = searchKakaoPlace(request);
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
    public PlaceResponse.SearchHistoryResult getSearchHistories(
            Long memberId,
            double latitude,
            double longitude
    ) {
        List<PlaceSearchHistory> activeHistories = placeSearchHistoryRepository
                .findTop5ByMemberIdOrderBySelectedAtDescIdDesc(memberId)
                .stream()
                .filter(history -> history.getPlace() != null)
                .filter(history -> !history.getPlace().isDeleted())
                .toList();

        if (activeHistories.isEmpty()) {
            return new PlaceResponse.SearchHistoryResult(List.of());
        }

        List<Long> placeIds = activeHistories.stream()
                .map(history -> history.getPlace().getId())
                .distinct()
                .toList();
        Map<Long, PlacePinInfo> pinInfosByPlaceId =
                pinQueryService.findPinInfosByPlaceIds(placeIds);

        List<PlaceResponse.SearchHistoryItem> items = activeHistories.stream()
                .map(history -> toSearchHistoryItem(
                        history,
                        latitude,
                        longitude,
                        pinInfosByPlaceId
                ))
                .toList();
        return new PlaceResponse.SearchHistoryResult(items);
    }

    private PlaceResponse.SearchHistoryItem toSearchHistoryItem(
            PlaceSearchHistory history,
            double userLatitude,
            double userLongitude,
            Map<Long, PlacePinInfo> pinInfosByPlaceId
    ) {
        Place place = history.getPlace();
        PlacePinInfo pinInfo = pinInfosByPlaceId.getOrDefault(place.getId(), NO_PIN_INFO);
        double distance = GeoDistanceCalculator.calculateMeters(
                userLatitude,
                userLongitude,
                history.getLocation().getY(),
                history.getLocation().getX()
        );

        return new PlaceResponse.SearchHistoryItem(
                history.getId(),
                place.getId(),
                history.getPlaceName(),
                history.getCategory(),
                history.getAddress(),
                place.getRoadAddress(),
                history.getLocation().getY(),
                history.getLocation().getX(),
                Math.toIntExact(Math.round(distance)),
                pinInfo.hasPin(),
                pinInfo.firstPinCreatorNickname(),
                history.getSelectedAt()
        );
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

    @Override
    public Optional<Place> findNearestActiveProviderPlaceSearchWithin(
            double latitude,
            double longitude
    ) {
        return placeQueryRepository.findNearestActiveProviderPlaceSearchWithin(
                latitude,
                longitude,
                PROVIDER_PLACE_SEARCH_DISTANCE_METERS
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

    private KakaoAddressSearchResponse searchKakaoAddress(String keyword) {
        try {
            return kakaoAddressSearchClient.search(keyword);
        } catch (KakaoClientTimeoutException exception) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_TIMEOUT, exception);
        } catch (KakaoClientException exception) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR, exception);
        }
    }

    private KakaoPlaceSearchResponse searchKakaoPlace(PlaceRequest.Search request) {
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

    private PlaceResponse.SearchResult toAddressSearchResult(
            KakaoAddressSearchResponse response,
            PlaceRequest.Search request
    ) {
        try {
            List<PlaceResponse.SearchItem> items = response.documents().stream()
                    .limit(MAX_SEARCH_RESULTS)
                    .map(document -> toAddressSearchItem(document, request))
                    .toList();
            return new PlaceResponse.SearchResult(items);
        } catch (RuntimeException exception) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR, exception);
        }
    }

    private PlaceResponse.SearchItem toAddressSearchItem(
            KakaoAddressSearchResponse.Document document,
            PlaceRequest.Search request
    ) {
        Objects.requireNonNull(document);
        String address = resolveAddress(document);
        String roadAddress = normalize(document.roadAddress() == null
                ? null
                : document.roadAddress().addressName());
        double latitude = parseCoordinate(document.y(), -90.0, 90.0);
        double longitude = parseCoordinate(document.x(), -180.0, 180.0);
        double distance = GeoDistanceCalculator.calculateMeters(
                request.latitude(),
                request.longitude(),
                latitude,
                longitude
        );

        return new PlaceResponse.SearchItem(
                ADDRESS_RESULT_TYPE,
                KAKAO_PROVIDER,
                null,
                roadAddress != null ? roadAddress : address,
                null,
                address,
                roadAddress,
                latitude,
                longitude,
                Math.toIntExact(Math.round(distance)),
                false,
                null
        );
    }

    private String resolveAddress(KakaoAddressSearchResponse.Document document) {
        String nestedAddress = document.address() == null
                ? null
                : normalize(document.address().addressName());
        String address = nestedAddress != null
                ? nestedAddress
                : normalize(document.addressName());
        return Objects.requireNonNull(address);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private double parseCoordinate(String value, double minimum, double maximum) {
        double coordinate = Double.parseDouble(Objects.requireNonNull(value));
        if (!Double.isFinite(coordinate) || coordinate < minimum || coordinate > maximum) {
            throw new IllegalArgumentException("Invalid Kakao coordinate");
        }
        return coordinate;
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
                PLACE_RESULT_TYPE,
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
