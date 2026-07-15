package com.example.plimap.domain.place.service.command.impl;

import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.repository.lock.PlaceLockRepository;
import com.example.plimap.domain.place.repository.query.PlaceQueryRepository;
import com.example.plimap.domain.place.service.command.PlaceCommandService;
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

    private static final double MAP_SELECTION_REUSE_DISTANCE_METERS = 20.0;
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final PlaceRepository placeRepository;
    private final PlaceQueryRepository placeQueryRepository;
    private final PlaceLockRepository placeLockRepository;

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
