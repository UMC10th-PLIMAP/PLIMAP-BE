package com.example.plimap.domain.place.service.command.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.repository.PlaceBookmarkRepository;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.repository.PlaceSearchHistoryRepository;
import com.example.plimap.domain.place.repository.query.PlaceQueryRepository;
import com.example.plimap.domain.place.service.query.PlaceLocationMetadataService;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlaceSearchHistoryCommandServiceTest {

    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceBookmarkRepository placeBookmarkRepository =
            mock(PlaceBookmarkRepository.class);
    private final PlaceSearchHistoryRepository placeSearchHistoryRepository =
            mock(PlaceSearchHistoryRepository.class);
    private final PlaceQueryRepository placeQueryRepository = mock(PlaceQueryRepository.class);
    private final PlacePersistenceService placePersistenceService =
            mock(PlacePersistenceService.class);
    private final PlaceQueryService placeQueryService = mock(PlaceQueryService.class);
    private final PlaceLocationMetadataService placeLocationMetadataService =
            mock(PlaceLocationMetadataService.class);
    private final PinQueryService pinQueryService = mock(PinQueryService.class);

    private PlaceCommandServiceImpl placeCommandService;

    @BeforeEach
    void setUp() {
        placeCommandService = new PlaceCommandServiceImpl(
                placeRepository,
                placeBookmarkRepository,
                placeSearchHistoryRepository,
                placeQueryRepository,
                placePersistenceService,
                placeQueryService,
                placeLocationMetadataService,
                pinQueryService
        );
    }

    @Test
    void 인증_사용자가_소유한_최근_검색_이력을_삭제한다() {
        when(placeSearchHistoryRepository.deleteByIdAndMemberId(10L, 7L))
                .thenReturn(1);

        placeCommandService.deleteSearchHistory(7L, 10L);

        verify(placeSearchHistoryRepository).deleteByIdAndMemberId(10L, 7L);
    }

    @Test
    void 이력이_없거나_다른_사용자가_소유하면_동일한_404_예외를_던진다() {
        when(placeSearchHistoryRepository.deleteByIdAndMemberId(10L, 7L))
                .thenReturn(0);

        assertThatThrownBy(() -> placeCommandService.deleteSearchHistory(7L, 10L))
                .isInstanceOf(PlaceException.class)
                .extracting(exception -> ((PlaceException) exception).getErrorCode())
                .isEqualTo(PlaceErrorCode.PLACE_SEARCH_HISTORY_NOT_FOUND);
    }
}
