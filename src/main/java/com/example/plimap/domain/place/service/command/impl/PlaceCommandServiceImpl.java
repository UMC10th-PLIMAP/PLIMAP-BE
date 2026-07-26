package com.example.plimap.domain.place.service.command.impl;

import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceBookmarkId;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.repository.PlaceBookmarkRepository;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.repository.PlaceSearchHistoryRepository;
import com.example.plimap.domain.place.repository.lock.PlaceLockRepository;
import com.example.plimap.domain.place.repository.query.PlaceQueryRepository;
import com.example.plimap.domain.place.service.command.PlaceCommandService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceCommandServiceImpl implements PlaceCommandService {

    private static final String KAKAO_PROVIDER = "KAKAO";
    private static final int ACCESS_RANGE_METERS = 500;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    private static final double MAP_SELECTION_REUSE_DISTANCE_METERS = 20.0;
    private static final PlacePinInfo NO_PIN_INFO = new PlacePinInfo(false, null, 0L);
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final PlaceRepository placeRepository;
    private final PlaceBookmarkRepository placeBookmarkRepository;
    private final PlaceSearchHistoryRepository placeSearchHistoryRepository;
    private final PlaceQueryRepository placeQueryRepository;
    private final PlaceLockRepository placeLockRepository;
    private final PinQueryService pinQueryService;

    @Override
    @Transactional
    public PlaceResponse.MapSelection confirmMapSelection(PlaceRequest.MapSelection request) {
        placeLockRepository.acquireMapSelectionLock();

        Place place = placeQueryRepository.findNearestActiveMapSelectionWithin(
                        request.latitude(),
                        request.longitude(),
                        MAP_SELECTION_REUSE_DISTANCE_METERS
                )
                .orElseGet(() -> createMapSelection(request));

        return PlaceResponse.MapSelection.from(place);
    }

    @Override
    @Transactional
    public PlaceResponse.Selection selectSearchPlace(
            Long memberId,
            PlaceRequest.Selection request
    ) {
        validateSelectionRequest(request);
        placeLockRepository.acquirePlaceSelectionLock(
                request.provider(),
                request.providerPlaceId()
        );

        Place place = placeRepository
                .findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                        request.provider(),
                        request.providerPlaceId()
                )
                .orElseGet(() -> createPlaceSearch(request));

        PlacePinInfo pinInfo = findPinInfo(place.getId());
        long pinCount = pinInfo.pinCount() == null ? 0L : pinInfo.pinCount();
        double distance = calculateDistance(
                request.userLatitude(),
                request.userLongitude(),
                place.getLocation().getY(),
                place.getLocation().getX()
        );
        int distanceMeters = Math.toIntExact(Math.round(distance));
        boolean bookmarkedByMe = placeBookmarkRepository.existsById(
                new PlaceBookmarkId(place.getId(), memberId)
        );
        saveSearchHistory(memberId, place.getId());

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

    private void saveSearchHistory(Long memberId, Long placeId) {
        placeLockRepository.acquirePlaceSearchHistoryLock(memberId);
        placeSearchHistoryRepository.upsert(memberId, placeId);
        placeSearchHistoryRepository.deleteExcessByMemberId(memberId);
    }

    private Place createMapSelection(PlaceRequest.MapSelection request) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(
                request.longitude(),
                request.latitude()
        ));
        Place place = Place.createMapSelection(
                resolvePlaceName(request),
                request.address(),
                request.roadAddress(),
                location
        );
        return placeRepository.save(place);
    }

    private Place createPlaceSearch(PlaceRequest.Selection request) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(
                request.longitude(),
                request.latitude()
        ));
        Place place = Place.createPlaceSearch(
                request.placeName(),
                request.category(),
                request.address(),
                request.roadAddress(),
                request.provider(),
                request.providerPlaceId(),
                location
        );
        return placeRepository.saveAndFlush(place);
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

    private double calculateDistance(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2
    ) {
        double latitudeDelta = Math.toRadians(latitude2 - latitude1);
        double longitudeDelta = Math.toRadians(longitude2 - longitude1);
        double haversine = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(latitude1))
                * Math.cos(Math.toRadians(latitude2))
                * Math.sin(longitudeDelta / 2)
                * Math.sin(longitudeDelta / 2);
        haversine = Math.min(1.0, Math.max(0.0, haversine));
        double angularDistance = 2 * Math.atan2(
                Math.sqrt(haversine),
                Math.sqrt(1 - haversine)
        );
        return EARTH_RADIUS_METERS * angularDistance;
    }

    private String resolvePlaceName(PlaceRequest.MapSelection request) {
        if (request.placeName() != null) {
            return request.placeName();
        }
        if (request.roadAddress() != null) {
            return request.roadAddress();
        }
        return request.address();
    }
}
