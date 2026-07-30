package com.example.plimap.domain.place.service.command.impl;

import com.example.plimap.domain.place.dto.PlaceAdministrativeRegion;
import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.repository.PlaceSearchHistoryRepository;
import com.example.plimap.domain.place.repository.lock.PlaceLockRepository;
import com.example.plimap.domain.place.repository.query.PlaceQueryRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class PlacePersistenceService {

    static final double MAP_SELECTION_REUSE_DISTANCE_METERS = 20.0;
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final PlaceRepository placeRepository;
    private final PlaceSearchHistoryRepository placeSearchHistoryRepository;
    private final PlaceQueryRepository placeQueryRepository;
    private final PlaceLockRepository placeLockRepository;

    @Transactional
    public Place createOrReuseMapSelection(
            PlaceRequest.MapSelection request,
            PlaceAdministrativeRegion region
    ) {
        placeLockRepository.acquireMapSelectionLock();
        return placeQueryRepository.findNearestActiveMapSelectionWithin(
                        request.latitude(),
                        request.longitude(),
                        MAP_SELECTION_REUSE_DISTANCE_METERS
                )
                .orElseGet(() -> createMapSelection(request, region));
    }

    @Transactional
    public Optional<Place> persistSearchSelection(
            Long memberId,
            PlaceRequest.Selection request,
            PlaceAdministrativeRegion region
    ) {
        placeLockRepository.acquirePlaceSelectionLock(
                request.provider(),
                request.providerPlaceId()
        );
        Optional<Place> place = placeRepository
                .findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
                        request.provider(),
                        request.providerPlaceId()
                )
                .or(() -> region == null
                        ? Optional.empty()
                        : Optional.of(createPlaceSearch(request, region)));
        place.ifPresent(foundPlace -> saveSearchHistory(memberId, foundPlace.getId()));
        return place;
    }

    private void saveSearchHistory(Long memberId, Long placeId) {
        placeLockRepository.acquirePlaceSearchHistoryLock(memberId);
        placeSearchHistoryRepository.upsert(memberId, placeId);
        placeSearchHistoryRepository.deleteExcessByMemberId(memberId);
    }

    private Place createMapSelection(
            PlaceRequest.MapSelection request,
            PlaceAdministrativeRegion region
    ) {
        Place place = Place.createMapSelection(
                resolvePlaceName(request),
                request.address(),
                request.roadAddress(),
                region.code(),
                region.sido(),
                region.sigungu(),
                region.eupMyeonDong(),
                point(request.latitude(), request.longitude())
        );
        return placeRepository.save(place);
    }

    private Place createPlaceSearch(
            PlaceRequest.Selection request,
            PlaceAdministrativeRegion region
    ) {
        Place place = Place.createPlaceSearch(
                request.placeName(),
                request.category(),
                request.address(),
                request.roadAddress(),
                region.code(),
                region.sido(),
                region.sigungu(),
                region.eupMyeonDong(),
                request.provider(),
                request.providerPlaceId(),
                point(request.latitude(), request.longitude())
        );
        return placeRepository.saveAndFlush(place);
    }

    private Point point(double latitude, double longitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
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
