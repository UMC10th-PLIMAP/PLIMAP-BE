package com.example.plimap.domain.place.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.service.query.impl.PlaceQueryServiceImpl;
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

    private PlaceQueryServiceImpl placeQueryService;

    @BeforeEach
    void setUp() {
        placeQueryService = new PlaceQueryServiceImpl(placeRepository);
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
}
