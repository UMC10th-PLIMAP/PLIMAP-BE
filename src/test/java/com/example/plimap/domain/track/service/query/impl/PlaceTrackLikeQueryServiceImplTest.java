package com.example.plimap.domain.track.service.query.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.track.repository.PlaceTrackLikeRepository;
import org.junit.jupiter.api.Test;

class PlaceTrackLikeQueryServiceImplTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long PLACE_ID = 2L;

    private final PlaceTrackLikeRepository placeTrackLikeRepository =
            mock(PlaceTrackLikeRepository.class);

    private final PlaceTrackLikeQueryServiceImpl placeTrackLikeQueryService =
            new PlaceTrackLikeQueryServiceImpl(placeTrackLikeRepository);

    @Test
    void 회원이_장소의_활성_장소_트랙을_좋아요했다면_true를_반환한다() {
        // given
        when(placeTrackLikeRepository
                .existsByIdMemberIdAndPlaceTrackPlaceIdAndPlaceTrackDeletedAtIsNull(
                        MEMBER_ID,
                        PLACE_ID
                )).thenReturn(true);

        // when
        boolean result =
                placeTrackLikeQueryService
                        .existsActivePlaceTrackLikedByMemberAtPlace(
                                MEMBER_ID,
                                PLACE_ID
                        );

        // then
        assertThat(result).isTrue();
        verify(placeTrackLikeRepository)
                .existsByIdMemberIdAndPlaceTrackPlaceIdAndPlaceTrackDeletedAtIsNull(
                        MEMBER_ID,
                        PLACE_ID
                );
    }

    @Test
    void 회원이_장소의_활성_장소_트랙을_좋아요하지_않았다면_false를_반환한다() {
        // given
        when(placeTrackLikeRepository
                .existsByIdMemberIdAndPlaceTrackPlaceIdAndPlaceTrackDeletedAtIsNull(
                        MEMBER_ID,
                        PLACE_ID
                )).thenReturn(false);

        // when
        boolean result =
                placeTrackLikeQueryService
                        .existsActivePlaceTrackLikedByMemberAtPlace(
                                MEMBER_ID,
                                PLACE_ID
                        );

        // then
        assertThat(result).isFalse();
        verify(placeTrackLikeRepository)
                .existsByIdMemberIdAndPlaceTrackPlaceIdAndPlaceTrackDeletedAtIsNull(
                        MEMBER_ID,
                        PLACE_ID
                );
    }
}
