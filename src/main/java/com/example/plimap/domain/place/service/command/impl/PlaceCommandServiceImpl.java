package com.example.plimap.domain.place.service.command.impl;

import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.place.dto.PlaceAdministrativeRegion;
import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceBookmarkId;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.repository.PlaceBookmarkRepository;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.repository.PlaceSearchHistoryRepository;
import com.example.plimap.domain.place.repository.query.PlaceQueryRepository;
import com.example.plimap.domain.place.service.command.PlaceCommandService;
import com.example.plimap.domain.place.service.query.PlaceLocationMetadataService;
import com.example.plimap.domain.place.util.PlaceAddressNormalizer;
import com.example.plimap.global.util.GeoDistanceCalculator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceCommandServiceImpl implements PlaceCommandService {

    private static final String KAKAO_PROVIDER = "KAKAO";
    private static final String PLACE_RESULT_TYPE = "PLACE";
    private static final String ADDRESS_RESULT_TYPE = "ADDRESS";
    private static final int ACCESS_RANGE_METERS = 500;
    private static final double DISTANCE_COMPARISON_EPSILON_METERS = 1e-6;
    private static final PlacePinInfo NO_PIN_INFO = new PlacePinInfo(false, null, 0L);

    private final PlaceRepository placeRepository;
    private final PlaceBookmarkRepository placeBookmarkRepository;
    private final PlaceSearchHistoryRepository placeSearchHistoryRepository;
    private final PlaceQueryRepository placeQueryRepository;
    private final PlacePersistenceService placePersistenceService;
    private final PlaceLocationMetadataService placeLocationMetadataService;
    private final PinQueryService pinQueryService;

    @Override
    public PlaceResponse.MapSelection confirmMapSelection(PlaceRequest.MapSelection request) {
        Place existingPlace = placeQueryRepository.findNearestActiveMapSelectionWithin(
                        request.latitude(),
                        request.longitude(),
                        PlacePersistenceService.MAP_SELECTION_REUSE_DISTANCE_METERS
                )
                .orElse(null);
        if (existingPlace != null) {
            return PlaceResponse.MapSelection.from(existingPlace);
        }

        PlaceAdministrativeRegion region =
                placeLocationMetadataService.getAdministrativeRegion(
                        request.latitude(),
                        request.longitude()
                );
        Place place = placePersistenceService.createOrReuseMapSelection(request, region);
        return PlaceResponse.MapSelection.from(place);
    }

    @Override
    public PlaceResponse.Selection selectSearchPlace(
            Long memberId,
            PlaceRequest.Selection request
    ) {
        validateSelectionRequest(request);
        String normalizedAddress = ADDRESS_RESULT_TYPE.equals(request.resultType())
                ? PlaceAddressNormalizer.normalize(request.address())
                : null;
        Place existingPlace = findExistingPlace(request, normalizedAddress).orElse(null);
        PlaceAdministrativeRegion region = existingPlace == null
                ? placeLocationMetadataService.getAdministrativeRegion(
                        request.latitude(),
                        request.longitude()
                )
                : null;
        Optional<Place> persistedPlace =
                persistSelection(memberId, request, normalizedAddress, region);
        if (persistedPlace.isEmpty()) {
            PlaceAdministrativeRegion retryRegion =
                    placeLocationMetadataService.getAdministrativeRegion(
                            request.latitude(),
                            request.longitude()
                    );
            persistedPlace = persistSelection(
                    memberId,
                    request,
                    normalizedAddress,
                    retryRegion
            );
        }
        Place place = persistedPlace.orElseThrow(
                () -> new PlaceException(PlaceErrorCode.PLACE_NOT_FOUND)
        );

        PlacePinInfo pinInfo = findPinInfo(place.getId());
        long pinCount = pinInfo.pinCount() == null ? 0L : pinInfo.pinCount();
        double distance = GeoDistanceCalculator.calculateMeters(
                request.userLatitude(),
                request.userLongitude(),
                place.getLocation().getY(),
                place.getLocation().getX()
        );
        int distanceMeters = Math.toIntExact(Math.round(distance));
        boolean bookmarkedByMe = placeBookmarkRepository.existsById(
                new PlaceBookmarkId(place.getId(), memberId)
        );
        return new PlaceResponse.Selection(
                place.getId(),
                place.getName(),
                place.getAddress(),
                place.getRoadAddress(),
                place.getSource(),
                distanceMeters,
                distance <= ACCESS_RANGE_METERS + DISTANCE_COMPARISON_EPSILON_METERS,
                pinInfo.hasPin(),
                pinInfo.firstPinCreatorNickname(),
                pinCount,
                bookmarkedByMe
        );
    }

    @Override
    @Transactional
    public void deleteSearchHistory(Long memberId, Long historyId) {
        int deletedCount = placeSearchHistoryRepository.deleteByIdAndMemberId(
                historyId,
                memberId
        );
        if (deletedCount == 0) {
            throw new PlaceException(PlaceErrorCode.PLACE_SEARCH_HISTORY_NOT_FOUND);
        }
    }

    private PlacePinInfo findPinInfo(Long placeId) {
        Map<Long, PlacePinInfo> pinInfos = pinQueryService.findPinInfosByPlaceIds(
                List.of(placeId)
        );
        return pinInfos.getOrDefault(placeId, NO_PIN_INFO);
    }

    private Optional<Place> findExistingPlace(
            PlaceRequest.Selection request,
            String normalizedAddress
    ) {
        if (ADDRESS_RESULT_TYPE.equals(request.resultType())) {
            return placeRepository.findBySourceAndNormalizedAddressAndDeletedAtIsNull(
                    PlaceSource.ADDRESS_SEARCH,
                    normalizedAddress
            );
        }
        return placeRepository.findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                request.provider(),
                request.providerPlaceId()
        );
    }

    private Optional<Place> persistSelection(
            Long memberId,
            PlaceRequest.Selection request,
            String normalizedAddress,
            PlaceAdministrativeRegion region
    ) {
        if (ADDRESS_RESULT_TYPE.equals(request.resultType())) {
            return placePersistenceService.persistAddressSearchSelection(
                    memberId,
                    request,
                    normalizedAddress,
                    region
            );
        }
        return placePersistenceService.persistPlaceSearchSelection(memberId, request, region);
    }

    private void validateSelectionRequest(PlaceRequest.Selection request) {
        if (request == null
                || !KAKAO_PROVIDER.equals(request.provider())
                || !isLatitude(request.latitude())
                || !isLongitude(request.longitude())
                || !isLatitude(request.userLatitude())
                || !isLongitude(request.userLongitude())
                || !isValidSelectionType(request)) {
            throw new PlaceException(PlaceErrorCode.PLACE_SELECTION_INVALID);
        }
    }

    private boolean isValidSelectionType(PlaceRequest.Selection request) {
        if (PLACE_RESULT_TYPE.equals(request.resultType())) {
            return !isBlankOrTooLong(request.providerPlaceId(), 255)
                    && !isBlankOrTooLong(request.placeName(), 100)
                    && !isBlankOrTooLong(request.address(), 255)
                    && !isTooLong(request.category(), 100)
                    && !isTooLong(request.roadAddress(), 255);
        }
        if (ADDRESS_RESULT_TYPE.equals(request.resultType())) {
            String resolvedName =
                    request.roadAddress() != null ? request.roadAddress() : request.address();
            return request.providerPlaceId() == null
                    && request.category() == null
                    && !isBlankOrTooLong(request.address(), 255)
                    && !isTooLong(request.roadAddress(), 255)
                    && !isBlankOrTooLong(resolvedName, 100);
        }
        return false;
    }

    private boolean isBlankOrTooLong(String value, int maxLength) {
        return value == null || value.isBlank() || value.length() > maxLength;
    }

    private boolean isTooLong(String value, int maxLength) {
        return value != null && value.length() > maxLength;
    }

    private boolean isLatitude(Double value) {
        return value != null && Double.isFinite(value) && value >= -90 && value <= 90;
    }

    private boolean isLongitude(Double value) {
        return value != null && Double.isFinite(value) && value >= -180 && value <= 180;
    }

}
