package com.example.plimap.domain.place.service.command.impl;

import com.example.plimap.domain.place.dto.PlaceAdministrativeRegion;
import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.repository.PlaceSearchHistoryRepository;
import com.example.plimap.domain.place.repository.lock.PlaceLockRepository;
import com.example.plimap.domain.place.repository.query.PlaceQueryRepository;
import com.example.plimap.domain.place.util.MapSelectionPlaceNameFormatter;
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
                .map(this::normalizeMapSelectionName)
                .orElseGet(() -> createMapSelection(request, region));
    }

    @Transactional
    public Place normalizeReusedMapSelectionName(Place place) {
        return normalizeMapSelectionName(place);
    }

    @Transactional
    public Optional<Place> persistPlaceSearchSelection(
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

    @Transactional
    public Optional<Place> persistAddressSearchSelection(
            Long memberId,
            PlaceRequest.Selection request,
            String normalizedAddress,
            PlaceAdministrativeRegion region
    ) {
        placeLockRepository.acquireAddressSelectionLock(normalizedAddress);
        Optional<Place> place = placeRepository
                .findBySourceAndNormalizedAddressAndDeletedAtIsNull(
                        PlaceSource.ADDRESS_SEARCH,
                        normalizedAddress
                )
                .or(() -> region == null
                        ? Optional.empty()
                        : Optional.of(createAddressSearch(
                                request,
                                normalizedAddress,
                                region
                        )));
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
                MapSelectionPlaceNameFormatter.format(
                        request.address(),
                        request.roadAddress(),
                        region.sido()
                ),
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

    private Place createAddressSearch(
            PlaceRequest.Selection request,
            String normalizedAddress,
            PlaceAdministrativeRegion region
    ) {
        Place place = Place.createAddressSearch(
                resolveAddressSearchPlaceName(request),
                request.address(),
                request.roadAddress(),
                normalizedAddress,
                region.code(),
                region.sido(),
                region.sigungu(),
                region.eupMyeonDong(),
                request.provider(),
                point(request.latitude(), request.longitude())
        );
        return placeRepository.saveAndFlush(place);
    }

    private Point point(double latitude, double longitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    private Place normalizeMapSelectionName(Place place) {
        String normalizedName = MapSelectionPlaceNameFormatter.format(
                place.getAddress(),
                place.getRoadAddress(),
                place.getSido()
        );
        if (!normalizedName.equals(place.getName())) {
            place.updateMapSelectionName(normalizedName);
            return placeRepository.save(place);
        }
        return place;
    }

    private String resolveAddressSearchPlaceName(PlaceRequest.Selection request) {
        return request.roadAddress() != null ? request.roadAddress() : request.address();
    }
}
