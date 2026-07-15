package com.example.plimap.domain.place.service.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.repository.lock.PlaceLockRepository;
import com.example.plimap.domain.place.repository.query.PlaceQueryRepository;
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
    private final PlaceQueryRepository placeQueryRepository = mock(PlaceQueryRepository.class);
    private final PlaceLockRepository placeLockRepository = mock(PlaceLockRepository.class);

    private PlaceCommandServiceImpl placeCommandService;

    @BeforeEach
    void setUp() {
        placeCommandService = new PlaceCommandServiceImpl(
                placeRepository,
                placeQueryRepository,
                placeLockRepository
        );
    }

    @Test
    void 잠금_후_20m_이내의_기존_장소를_재사용한다() {
        PlaceRequest.MapSelection request = request("새 장소명", "도로명 주소");
        Place existingPlace = place(1L, "기존 장소", 37.5283, 126.9326);
        when(placeQueryRepository.findNearestActiveMapSelectionWithin(37.5283, 126.9326, 20.0))
                .thenReturn(Optional.of(existingPlace));

        PlaceResponse.MapSelection result = placeCommandService.confirmMapSelection(request);

        assertThat(result.placeId()).isEqualTo(1L);
        assertThat(result.placeName()).isEqualTo("기존 장소");
        assertThat(result.latitude()).isEqualTo(37.5283);
        assertThat(result.longitude()).isEqualTo(126.9326);
        InOrder flow = inOrder(placeLockRepository, placeQueryRepository);
        flow.verify(placeLockRepository).acquireMapSelectionLock();
        flow.verify(placeQueryRepository)
                .findNearestActiveMapSelectionWithin(37.5283, 126.9326, 20.0);
        verify(placeRepository, never()).save(any(Place.class));
    }

    @Test
    void 기존_장소가_없으면_placeName을_사용해_MAP_SELECTION_장소를_생성한다() {
        PlaceRequest.MapSelection request = request("  물빛무대 앞 광장  ", "  여의동로  ");
        when(placeQueryRepository.findNearestActiveMapSelectionWithin(37.5283, 126.9326, 20.0))
                .thenReturn(Optional.empty());
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);

        PlaceResponse.MapSelection result = placeCommandService.confirmMapSelection(request);

        verify(placeRepository).save(captor.capture());
        Place createdPlace = captor.getValue();
        assertThat(createdPlace.getName()).isEqualTo("물빛무대 앞 광장");
        assertThat(createdPlace.getAddress()).isEqualTo("지번 주소");
        assertThat(createdPlace.getRoadAddress()).isEqualTo("여의동로");
        assertThat(createdPlace.getSource()).isEqualTo(PlaceSource.MAP_SELECTION);
        assertThat(createdPlace.getPlaceProvider()).isNull();
        assertThat(createdPlace.getProviderPlaceId()).isNull();
        assertThat(createdPlace.getCategory()).isNull();
        assertThat(createdPlace.getLocation().getSRID()).isEqualTo(4326);
        assertThat(createdPlace.getLocation().getX()).isEqualTo(126.9326);
        assertThat(createdPlace.getLocation().getY()).isEqualTo(37.5283);
        assertThat(result.placeName()).isEqualTo("물빛무대 앞 광장");
    }

    @Test
    void placeName이_blank이면_roadAddress를_장소명으로_사용한다() {
        PlaceRequest.MapSelection request = request("   ", "  도로명 주소  ");
        when(placeQueryRepository.findNearestActiveMapSelectionWithin(37.5283, 126.9326, 20.0))
                .thenReturn(Optional.empty());
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);

        placeCommandService.confirmMapSelection(request);

        verify(placeRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("도로명 주소");
    }

    @Test
    void placeName과_roadAddress가_없으면_address를_장소명으로_사용한다() {
        PlaceRequest.MapSelection request =
                new PlaceRequest.MapSelection(37.5283, 126.9326, null, "  지번 주소  ", " ");
        when(placeQueryRepository.findNearestActiveMapSelectionWithin(37.5283, 126.9326, 20.0))
                .thenReturn(Optional.empty());
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);

        placeCommandService.confirmMapSelection(request);

        verify(placeRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("지번 주소");
    }

    private PlaceRequest.MapSelection request(String placeName, String roadAddress) {
        return new PlaceRequest.MapSelection(
                37.5283,
                126.9326,
                placeName,
                "  지번 주소  ",
                roadAddress
        );
    }

    private Place place(Long id, String name, double latitude, double longitude) {
        Place place = mock(Place.class);
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        when(place.getId()).thenReturn(id);
        when(place.getName()).thenReturn(name);
        when(place.getSource()).thenReturn(PlaceSource.MAP_SELECTION);
        when(place.getLocation()).thenReturn(location);
        return place;
    }
}
