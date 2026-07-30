package com.example.plimap.domain.place.service.command.impl;

import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.place.dto.PlaceAdministrativeRegion;
import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceBookmarkId;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.repository.PlaceBookmarkRepository;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.repository.PlaceSearchHistoryRepository;
import com.example.plimap.domain.place.repository.query.PlaceQueryRepository;
import com.example.plimap.domain.place.service.command.PlaceCommandService;
import com.example.plimap.domain.place.service.query.PlaceLocationMetadataService;
import com.example.plimap.global.util.GeoDistanceCalculator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceCommandServiceImpl implements PlaceCommandService {

    private static final String KAKAO_PROVIDER = "KAKAO";
    private static final int ACCESS_RANGE_METERS = 500;
    private static final double MAP_SELECTION_REUSE_DISTANCE_METERS = 20.0;
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
                        MAP_SELECTION_REUSE_DISTANCE_METERS
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
        Place existingPlace = placeRepository
                .findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                        request.provider(),
                        request.providerPlaceId()
                )
                .orElse(null);
        PlaceAdministrativeRegion region = existingPlace == null
                ? placeLocationMetadataService.getAdministrativeRegion(
                        request.latitude(),
                        request.longitude()
                )
                : null;
        Place place = placePersistenceService.persistSearchSelection(
                memberId,
                request,
                region
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
                distance <= ACCESS_RANGE_METERS,
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

    private void validateSelectionRequest(PlaceRequest.Selection request) {
        if (request == null
                || !KAKAO_PROVIDER.equals(request.provider())
                || isBlankOrTooLong(request.providerPlaceId(), 255)
                || isBlankOrTooLong(request.placeName(), 100)
                || isBlankOrTooLong(request.address(), 255)
                || isTooLong(request.category(), 100)
                || isTooLong(request.roadAddress(), 255)
                || !isLatitude(request.latitude())
                || !isLongitude(request.longitude())
                || !isLatitude(request.userLatitude())
                || !isLongitude(request.userLongitude())) {
            throw new PlaceException(PlaceErrorCode.PLACE_SELECTION_INVALID);
        }
    }

    private boolean isBlankOrTooLong(String value, int maxLength) {
        return value == null || value.length() > maxLength;
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
