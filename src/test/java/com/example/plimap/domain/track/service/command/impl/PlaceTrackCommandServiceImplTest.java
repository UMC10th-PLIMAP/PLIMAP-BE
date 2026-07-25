package com.example.plimap.domain.track.service.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.PlaceTrackLike;
import com.example.plimap.domain.track.entity.PlaceTrackLikeId;
import com.example.plimap.domain.track.entity.Track;
import com.example.plimap.domain.track.exception.TrackErrorCode;
import com.example.plimap.domain.track.exception.TrackException;
import com.example.plimap.domain.track.repository.PlaceTrackLikeRepository;
import com.example.plimap.domain.track.repository.PlaceTrackRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PlaceTrackCommandServiceImplTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long PLACE_TRACK_ID = 10L;

    private final PlaceTrackRepository placeTrackRepository =
            mock(PlaceTrackRepository.class);
    private final PlaceTrackLikeRepository placeTrackLikeRepository =
            mock(PlaceTrackLikeRepository.class);
    private final PlaceTrackCommandServiceImpl placeTrackCommandService =
            new PlaceTrackCommandServiceImpl(
                    placeTrackRepository,
                    placeTrackLikeRepository
            );

    @Test
    void 활성_PlaceTrack에_좋아요를_등록하면_상태와_개수를_반환한다() {
        PlaceTrack placeTrack = placeTrack(0);
        PlaceTrackLikeId likeId = likeId(MEMBER_ID);
        when(placeTrackRepository.findActiveByIdForUpdate(PLACE_TRACK_ID))
                .thenReturn(Optional.of(placeTrack));
        when(placeTrackLikeRepository.existsById(likeId)).thenReturn(false);

        PlaceTrackResponse.PlaceTrackLikeResult result =
                placeTrackCommandService.createPlaceTrackLike(
                        MEMBER_ID,
                        PLACE_TRACK_ID
                );

        assertThat(result.placeTrackId()).isEqualTo(PLACE_TRACK_ID);
        assertThat(result.isLiked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(1);
        assertThat(placeTrack.getLikeCount()).isEqualTo(1);
        verify(placeTrackLikeRepository).save(any(PlaceTrackLike.class));
    }

    @Test
    void 동일_사용자가_좋아요를_중복_등록하면_예외가_발생한다() {
        PlaceTrack placeTrack = placeTrack(3);
        PlaceTrackLikeId likeId = likeId(MEMBER_ID);
        when(placeTrackRepository.findActiveByIdForUpdate(PLACE_TRACK_ID))
                .thenReturn(Optional.of(placeTrack));
        when(placeTrackLikeRepository.existsById(likeId)).thenReturn(true);

        assertTrackError(
                () -> placeTrackCommandService.createPlaceTrackLike(
                        MEMBER_ID,
                        PLACE_TRACK_ID
                ),
                TrackErrorCode.PLACE_TRACK_ALREADY_LIKED
        );

        assertThat(placeTrack.getLikeCount()).isEqualTo(3);
        verify(placeTrackLikeRepository, never()).save(any());
    }

    @Test
    void 존재하지_않거나_삭제된_PlaceTrack에_좋아요를_등록하면_예외가_발생한다() {
        when(placeTrackRepository.findActiveByIdForUpdate(PLACE_TRACK_ID))
                .thenReturn(Optional.empty());

        assertTrackError(
                () -> placeTrackCommandService.createPlaceTrackLike(
                        MEMBER_ID,
                        PLACE_TRACK_ID
                ),
                TrackErrorCode.PLACE_TRACK_NOT_FOUND
        );

        verify(placeTrackLikeRepository, never()).save(any());
    }

    @Test
    void 좋아요_저장에_실패하면_좋아요_수는_증가하지_않는다() {
        PlaceTrack placeTrack = placeTrack(0);
        when(placeTrackRepository.findActiveByIdForUpdate(PLACE_TRACK_ID))
                .thenReturn(Optional.of(placeTrack));
        when(placeTrackLikeRepository.existsById(likeId(MEMBER_ID)))
                .thenReturn(false);
        when(placeTrackLikeRepository.save(any(PlaceTrackLike.class)))
                .thenThrow(new RuntimeException("save failed"));

        assertThatThrownBy(() ->
                placeTrackCommandService.createPlaceTrackLike(
                        MEMBER_ID,
                        PLACE_TRACK_ID
                )).isInstanceOf(RuntimeException.class);

        assertThat(placeTrack.getLikeCount()).isZero();
    }

    @Test
    void 본인이_등록한_좋아요를_삭제하면_상태와_개수를_반환한다() {
        PlaceTrack placeTrack = placeTrack(2);
        PlaceTrackLike like = mock(PlaceTrackLike.class);
        when(placeTrackRepository.findActiveByIdForUpdate(PLACE_TRACK_ID))
                .thenReturn(Optional.of(placeTrack));
        when(placeTrackLikeRepository.findById(likeId(MEMBER_ID)))
                .thenReturn(Optional.of(like));

        PlaceTrackResponse.PlaceTrackLikeResult result =
                placeTrackCommandService.deletePlaceTrackLike(
                        MEMBER_ID,
                        PLACE_TRACK_ID
                );

        assertThat(result.placeTrackId()).isEqualTo(PLACE_TRACK_ID);
        assertThat(result.isLiked()).isFalse();
        assertThat(result.likeCount()).isEqualTo(1);
        assertThat(placeTrack.getLikeCount()).isEqualTo(1);
        verify(placeTrackLikeRepository).delete(like);
    }

    @Test
    void 좋아요를_등록하지_않은_사용자가_삭제하면_예외가_발생한다() {
        PlaceTrack placeTrack = placeTrack(2);
        when(placeTrackRepository.findActiveByIdForUpdate(PLACE_TRACK_ID))
                .thenReturn(Optional.of(placeTrack));
        when(placeTrackLikeRepository.findById(likeId(MEMBER_ID)))
                .thenReturn(Optional.empty());

        assertTrackError(
                () -> placeTrackCommandService.deletePlaceTrackLike(
                        MEMBER_ID,
                        PLACE_TRACK_ID
                ),
                TrackErrorCode.PLACE_TRACK_LIKE_NOT_FOUND
        );

        assertThat(placeTrack.getLikeCount()).isEqualTo(2);
        verify(placeTrackLikeRepository, never()).delete(any());
    }

    @Test
    void 다른_사용자의_좋아요만_있으면_삭제할_수_없다() {
        PlaceTrack placeTrack = placeTrack(1);
        Long otherMemberId = 2L;
        when(placeTrackRepository.findActiveByIdForUpdate(PLACE_TRACK_ID))
                .thenReturn(Optional.of(placeTrack));
        when(placeTrackLikeRepository.findById(likeId(MEMBER_ID)))
                .thenReturn(Optional.empty());
        when(placeTrackLikeRepository.findById(likeId(otherMemberId)))
                .thenReturn(Optional.of(mock(PlaceTrackLike.class)));

        assertTrackError(
                () -> placeTrackCommandService.deletePlaceTrackLike(
                        MEMBER_ID,
                        PLACE_TRACK_ID
                ),
                TrackErrorCode.PLACE_TRACK_LIKE_NOT_FOUND
        );

        assertThat(placeTrack.getLikeCount()).isEqualTo(1);
        verify(placeTrackLikeRepository, never()).delete(any());
    }

    @Test
    void 좋아요_수가_0이면_삭제해도_음수가_되지_않는다() {
        PlaceTrack placeTrack = placeTrack(0);
        PlaceTrackLike like = mock(PlaceTrackLike.class);
        when(placeTrackRepository.findActiveByIdForUpdate(PLACE_TRACK_ID))
                .thenReturn(Optional.of(placeTrack));
        when(placeTrackLikeRepository.findById(likeId(MEMBER_ID)))
                .thenReturn(Optional.of(like));

        PlaceTrackResponse.PlaceTrackLikeResult result =
                placeTrackCommandService.deletePlaceTrackLike(
                        MEMBER_ID,
                        PLACE_TRACK_ID
                );

        assertThat(result.likeCount()).isZero();
        assertThat(placeTrack.getLikeCount()).isZero();
    }

    @Test
    void 좋아요_삭제에_실패하면_좋아요_수는_감소하지_않는다() {
        PlaceTrack placeTrack = placeTrack(2);
        PlaceTrackLike like = mock(PlaceTrackLike.class);
        when(placeTrackRepository.findActiveByIdForUpdate(PLACE_TRACK_ID))
                .thenReturn(Optional.of(placeTrack));
        when(placeTrackLikeRepository.findById(likeId(MEMBER_ID)))
                .thenReturn(Optional.of(like));
        org.mockito.Mockito.doThrow(new RuntimeException("delete failed"))
                .when(placeTrackLikeRepository)
                .delete(like);

        assertThatThrownBy(() ->
                placeTrackCommandService.deletePlaceTrackLike(
                        MEMBER_ID,
                        PLACE_TRACK_ID
                )).isInstanceOf(RuntimeException.class);

        assertThat(placeTrack.getLikeCount()).isEqualTo(2);
    }

    private PlaceTrack placeTrack(int likeCount) {
        PlaceTrack placeTrack = PlaceTrack.create(
                mock(Place.class),
                mock(Track.class)
        );
        ReflectionTestUtils.setField(placeTrack, "id", PLACE_TRACK_ID);
        ReflectionTestUtils.setField(placeTrack, "likeCount", likeCount);
        return placeTrack;
    }

    private PlaceTrackLikeId likeId(Long memberId) {
        return new PlaceTrackLikeId(PLACE_TRACK_ID, memberId);
    }

    private void assertTrackError(Runnable operation, TrackErrorCode errorCode) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(TrackException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
