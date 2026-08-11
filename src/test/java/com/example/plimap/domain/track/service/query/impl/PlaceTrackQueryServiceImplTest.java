package com.example.plimap.domain.track.service.query.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.pin.validator.PinLocationValidator;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
import com.example.plimap.domain.track.dto.LikedPlaceTrackQueryResult;
import com.example.plimap.domain.track.dto.PlaceTrackQueryResult;
import com.example.plimap.domain.track.dto.request.PlaceTrackRequest;
import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.PlaceTrackLikeId;
import com.example.plimap.domain.track.entity.Track;
import com.example.plimap.domain.track.enums.PlaceTrackSort;
import com.example.plimap.domain.track.exception.TrackErrorCode;
import com.example.plimap.domain.track.exception.TrackException;
import com.example.plimap.domain.track.repository.PlaceTrackLikeRepository;
import com.example.plimap.domain.track.repository.PlaceTrackRepository;
import com.example.plimap.domain.track.repository.query.PlaceTrackQueryRepository;
import com.example.plimap.domain.track.service.query.PlaceTrackLikeQueryService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

class PlaceTrackQueryServiceImplTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long PLACE_ID = 2L;

    private final MemberQueryService memberQueryService = mock(MemberQueryService.class);
    private final PinQueryService pinQueryService = mock(PinQueryService.class);
    private final PlaceQueryService placeQueryService = mock(PlaceQueryService.class);
    private final PinLocationValidator pinLocationValidator =
            mock(PinLocationValidator.class);
    private final PlaceTrackRepository placeTrackRepository =
            mock(PlaceTrackRepository.class);
    private final PlaceTrackLikeRepository placeTrackLikeRepository =
            mock(PlaceTrackLikeRepository.class);
    private final PlaceTrackQueryRepository placeTrackQueryRepository =
            mock(PlaceTrackQueryRepository.class);
    private final PlaceTrackLikeQueryService placeTrackLikeQueryService =
            mock(PlaceTrackLikeQueryService.class);

    private final PlaceTrackQueryServiceImpl placeTrackQueryService =
            new PlaceTrackQueryServiceImpl(
                    memberQueryService,
                    pinQueryService,
                    placeQueryService,
                    pinLocationValidator,
                    placeTrackRepository,
                    placeTrackLikeRepository,
                    placeTrackQueryRepository,
                    placeTrackLikeQueryService
            );

    @Test
    void 좋아요한_장소별_곡_목록과_페이지_정보를_반환한다() {
        PageRequest pageable = PageRequest.of(0, 2);
        when(placeTrackQueryRepository.findLikedPlaceTracks(
                MEMBER_ID,
                pageable
        )).thenReturn(new SliceImpl<>(
                List.of(
                        likedTrack(20L, "두 번째 곡", 12),
                        likedTrack(10L, "첫 번째 곡", 5)
                ),
                pageable,
                true
        ));

        PlaceTrackResponse.LikedPlaceTrackListResult result =
                placeTrackQueryService.getLikedPlaceTracks(MEMBER_ID, 0, 2);

        assertThat(result.tracks())
                .extracting(PlaceTrackResponse.LikedPlaceTrackItem::placeTrackId)
                .containsExactly(20L, 10L);
        assertThat(result.tracks().getFirst().trackName()).isEqualTo("두 번째 곡");
        assertThat(result.tracks().getFirst().artistName()).isEqualTo("아티스트");
        assertThat(result.tracks().getFirst().artworkUrl())
                .isEqualTo("https://image.example/20");
        assertThat(result.tracks().getFirst().likeCount()).isEqualTo(12);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
        verify(placeTrackQueryRepository).findLikedPlaceTracks(
                MEMBER_ID,
                pageable
        );
    }

    @Test
    void 좋아요한_장소별_곡이_없으면_빈_목록을_반환한다() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(placeTrackQueryRepository.findLikedPlaceTracks(
                MEMBER_ID,
                pageable
        )).thenReturn(new SliceImpl<>(List.of(), pageable, false));

        PlaceTrackResponse.LikedPlaceTrackListResult result =
                placeTrackQueryService.getLikedPlaceTracks(MEMBER_ID, 0, 20);

        assertThat(result.tracks()).isEmpty();
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void 반경_500미터_이내이면_장소_노래_상세와_사용자_좋아요를_반환한다() {
        givenDetailPlaceTrack(500.0);
        when(placeTrackLikeRepository.existsById(
                new PlaceTrackLikeId(10L, MEMBER_ID)
        )).thenReturn(true);

        PlaceTrackResponse.PlaceTrackDetail result =
                placeTrackQueryService.getPlaceTrackDetail(
                        MEMBER_ID,
                        10L,
                        userLocation(),
                        null
                );

        assertThat(result.placeTrackId()).isEqualTo(10L);
        assertThat(result.trackId()).isEqualTo(20L);
        assertThat(result.youtubeVideoId()).isEqualTo("youtube-video-id");
        assertThat(result.title()).isEqualTo("LOVE ATTACK");
        assertThat(result.artist()).isEqualTo("RESCENE");
        assertThat(result.albumImageUrl())
                .isEqualTo("https://example.com/love-attack.png");
        assertThat(result.likeCount()).isEqualTo(33);
        assertThat(result.userLike()).isTrue();
    }

    @Test
    void 반경_밖이지만_본인_활성_PIN이_있으면_상세_조회에_성공한다() {
        Place place = givenDetailPlaceTrack(500.1);
        Member member = mock(Member.class);
        when(memberQueryService.getActiveMember(MEMBER_ID)).thenReturn(member);
        when(pinQueryService.validatePlacePinAccessByMember(member, place))
                .thenReturn(true);
        when(placeTrackLikeRepository.existsById(
                new PlaceTrackLikeId(10L, MEMBER_ID)
        )).thenReturn(false);

        PlaceTrackResponse.PlaceTrackDetail result =
                placeTrackQueryService.getPlaceTrackDetail(
                        MEMBER_ID,
                        10L,
                        userLocation(),
                        null
                );

        assertThat(result.likeCount()).isEqualTo(33);
        assertThat(result.userLike()).isFalse();
    }

    @Test
    void 반경_밖이지만_해당_장소의_PlaceTrack_좋아요가_있으면_상세_조회에_성공한다() {
        givenDetailPlaceTrack(500.1);
        when(placeTrackLikeQueryService
                .existsActivePlaceTrackLikedByMemberAtPlace(MEMBER_ID, PLACE_ID))
                .thenReturn(true);

        PlaceTrackResponse.PlaceTrackDetail result =
                placeTrackQueryService.getPlaceTrackDetail(
                        MEMBER_ID,
                        10L,
                        userLocation(),
                        null
                );

        assertThat(result.placeTrackId()).isEqualTo(10L);
    }

    @Test
    void 반경_밖이지만_유효한_친구_피드_토큰이_있으면_상세_조회에_성공한다() {
        givenDetailPlaceTrack(500.1);
        when(pinQueryService.hasValidFeedToken("token", MEMBER_ID, PLACE_ID))
                .thenReturn(true);

        PlaceTrackResponse.PlaceTrackDetail result =
                placeTrackQueryService.getPlaceTrackDetail(
                        MEMBER_ID,
                        10L,
                        userLocation(),
                        "token"
                );

        assertThat(result.placeTrackId()).isEqualTo(10L);
    }

    @Test
    void 모든_접근_조건을_충족하지_못하고_토큰이_없으면_접근을_거부한다() {
        givenDetailPlaceTrack(500.1);

        assertAccessDenied(null);
    }

    @Test
    void 모든_접근_조건을_충족하지_못하고_토큰이_유효하지_않으면_접근을_거부한다() {
        givenDetailPlaceTrack(500.1);

        assertAccessDenied("invalid-token");
        verify(pinQueryService).hasValidFeedToken(
                "invalid-token",
                MEMBER_ID,
                PLACE_ID
        );
    }

    @Test
    void 다른_회원의_친구_피드_토큰이면_접근을_거부한다() {
        givenDetailPlaceTrack(500.1);

        assertAccessDenied("other-member-token");
        verify(pinQueryService).hasValidFeedToken(
                "other-member-token",
                MEMBER_ID,
                PLACE_ID
        );
    }

    @Test
    void 다른_장소의_친구_피드_토큰이면_접근을_거부한다() {
        givenDetailPlaceTrack(500.1);

        assertAccessDenied("other-place-token");
        verify(pinQueryService).hasValidFeedToken(
                "other-place-token",
                MEMBER_ID,
                PLACE_ID
        );
    }

    @Test
    void 장소_노래가_없으면_PLACE_TRACK_NOT_FOUND_예외가_발생한다() {
        when(placeTrackRepository.findDetailByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                placeTrackQueryService.getPlaceTrackDetail(
                        MEMBER_ID,
                        10L,
                        userLocation(),
                        null
                ))
                .isInstanceOfSatisfying(TrackException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(TrackErrorCode.PLACE_TRACK_NOT_FOUND));

        verifyNoInteractions(
                placeTrackLikeRepository,
                placeTrackLikeQueryService,
                pinQueryService,
                pinLocationValidator
        );
    }

    @Test
    void 존재하지_않는_장소이면_장소_예외를_전파한다() {
        PlaceException exception = new PlaceException(PlaceErrorCode.PLACE_NOT_FOUND);
        when(placeQueryService.getActivePlace(PLACE_ID)).thenThrow(exception);

        assertThatThrownBy(() ->
                placeTrackQueryService.getPlaceTracks(MEMBER_ID, PLACE_ID, request()))
                .isSameAs(exception);

        verifyNoInteractions(
                pinLocationValidator,
                placeTrackQueryRepository
        );
    }

    @Test
    void 장소에_곡이_없으면_빈_목록을_반환한다() {
        givenPlaceAndDistance(100.0);
        givenTracks(List.of(), false);

        PlaceTrackResponse.PlaceTrackListResult result =
                placeTrackQueryService.getPlaceTracks(MEMBER_ID, PLACE_ID, request());

        assertThat(result.tracks()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void 반경_이내이면_좋아요_정보를_반환한다() {
        givenPlaceAndDistance(499.9);
        givenTracks(List.of(track(10L, 5, true)), false);

        PlaceTrackResponse.PlaceTrackListResult result =
                placeTrackQueryService.getPlaceTracks(MEMBER_ID, PLACE_ID, request());

        assertThat(result.isWithinRadius()).isTrue();
        assertThat(result.isTrackDetailAccessible()).isTrue();
        assertThat(result.tracks().getFirst().pinCount()).isEqualTo(1);
        assertThat(result.tracks().getFirst().likeCount()).isEqualTo(5);
        assertThat(result.tracks().getFirst().isLiked()).isTrue();
        assertThat(result.tracks().getFirst().pinByMe()).isFalse();
        verifyNoInteractions(
                memberQueryService,
                placeTrackLikeRepository
        );
    }

    @Test
    void 정확히_500미터이면_반경_이내로_처리한다() {
        givenPlaceAndDistance(500.0);
        givenTracks(List.of(track(10L, 5, true)), false);

        PlaceTrackResponse.PlaceTrackListResult result =
                placeTrackQueryService.getPlaceTracks(MEMBER_ID, PLACE_ID, request());

        assertThat(result.distance()).isEqualTo(500.0);
        assertThat(result.isWithinRadius()).isTrue();
        assertThat(result.isTrackDetailAccessible()).isTrue();
        assertThat(result.tracks().getFirst().pinCount()).isEqualTo(1);
        assertThat(result.tracks().getFirst().likeCount()).isEqualTo(5);
    }

    @Test
    void 반경_밖이지만_장소곡_좋아요가_있으면_집계_정보를_반환한다() {
        givenPlaceAndDistance(500.1);
        when(placeTrackLikeRepository
                .existsByIdMemberIdAndPlaceTrackPlaceIdAndPlaceTrackDeletedAtIsNull(
                        MEMBER_ID,
                        PLACE_ID
                )).thenReturn(true);
        givenTracks(
                List.of(
                        track(20L, 10, true),
                        track(10L, 5, false)
                ),
                false
        );

        PlaceTrackResponse.PlaceTrackListResult result =
                placeTrackQueryService.getPlaceTracks(MEMBER_ID, PLACE_ID, request());

        assertThat(result.isWithinRadius()).isFalse();
        assertThat(result.isTrackDetailAccessible()).isTrue();
        assertThat(result.tracks())
                .extracting(PlaceTrackResponse.PlaceTrackItem::placeTrackId)
                .containsExactly(20L, 10L);
        assertThat(result.tracks().get(0).pinCount()).isEqualTo(1);
        assertThat(result.tracks().get(0).likeCount()).isEqualTo(10);
        assertThat(result.tracks().get(1).pinCount()).isEqualTo(1);
        assertThat(result.tracks().get(1).likeCount()).isEqualTo(5);
        assertThat(result.tracks().get(0).isLiked()).isTrue();
        assertThat(result.tracks().get(1).isLiked()).isFalse();
        verifyNoInteractions(memberQueryService);
    }

    @Test
    void 내_활성_PIN과_연결된_PlaceTrack만_pinByMe가_true이다() {
        givenPlaceAndDistance(100.0);
        givenTracks(
                List.of(
                        track(20L, 10, true),
                        track(10L, 5, false)
                ),
                false
        );
        when(pinQueryService.findActivePlaceTrackIdByPlaceIdAndMemberId(
                PLACE_ID,
                MEMBER_ID
        )).thenReturn(Optional.of(20L));

        PlaceTrackResponse.PlaceTrackListResult result =
                placeTrackQueryService.getPlaceTracks(MEMBER_ID, PLACE_ID, request());

        assertThat(result.tracks())
                .extracting(PlaceTrackResponse.PlaceTrackItem::pinByMe)
                .containsExactly(true, false);
        verify(pinQueryService).findActivePlaceTrackIdByPlaceIdAndMemberId(
                PLACE_ID,
                MEMBER_ID
        );
    }

    @Test
    void 장소에_내_활성_PIN이_없으면_모든_PlaceTrack의_pinByMe가_false이다() {
        givenPlaceAndDistance(100.0);
        givenTracks(
                List.of(
                        track(20L, 10, true),
                        track(10L, 5, false)
                ),
                false
        );
        when(pinQueryService.findActivePlaceTrackIdByPlaceIdAndMemberId(
                PLACE_ID,
                MEMBER_ID
        )).thenReturn(Optional.empty());

        PlaceTrackResponse.PlaceTrackListResult result =
                placeTrackQueryService.getPlaceTracks(MEMBER_ID, PLACE_ID, request());

        assertThat(result.tracks())
                .extracting(PlaceTrackResponse.PlaceTrackItem::pinByMe)
                .containsOnly(false);
    }

    @Test
    void 반경_밖이지만_PIN_접근이_허용되면_조회한다() {
        Place place = givenPlaceAndDistance(500.1);
        Member member = mock(Member.class);
        givenNoLikedPlaceTrack();
        when(memberQueryService.getActiveMember(MEMBER_ID)).thenReturn(member);
        when(pinQueryService.validatePlacePinAccessByMember(member, place))
                .thenReturn(true);
        givenTracks(List.of(track(10L, 5, true)), false);

        PlaceTrackResponse.PlaceTrackListResult result =
                placeTrackQueryService.getPlaceTracks(MEMBER_ID, PLACE_ID, request());

        assertThat(result.isWithinRadius()).isFalse();
        assertThat(result.isTrackDetailAccessible()).isTrue();
        assertThat(result.tracks().getFirst().pinCount()).isEqualTo(1);
        assertThat(result.tracks().getFirst().likeCount()).isEqualTo(5);
        assertThat(result.tracks().getFirst().isLiked()).isTrue();
    }

    @Test
    void 반경_밖이고_예외_접근_조건이_없어도_목록은_반환하고_집계_정보를_가린다() {
        Place place = givenPlaceAndDistance(500.1);
        Member member = mock(Member.class);
        givenNoLikedPlaceTrack();
        when(memberQueryService.getActiveMember(MEMBER_ID)).thenReturn(member);
        when(pinQueryService.validatePlacePinAccessByMember(member, place))
                .thenReturn(false);
        givenTracks(List.of(track(10L, 5, true)), false);

        PlaceTrackResponse.PlaceTrackListResult result =
                placeTrackQueryService.getPlaceTracks(MEMBER_ID, PLACE_ID, request());

        assertThat(result.isWithinRadius()).isFalse();
        assertThat(result.isTrackDetailAccessible()).isFalse();
        assertThat(result.tracks()).hasSize(1);
        assertThat(result.tracks().getFirst().pinCount()).isEqualTo(1);
        assertThat(result.tracks().getFirst().likeCount()).isNull();
        assertThat(result.tracks().getFirst().isLiked()).isTrue();
    }

    @Test
    void size보다_한_건_더_조회되면_hasNext를_반환한다() {
        givenPlaceAndDistance(100.0);
        givenTracks(List.of(track(10L, 5, true)), true);

        PlaceTrackResponse.PlaceTrackListResult result =
                placeTrackQueryService.getPlaceTracks(MEMBER_ID, PLACE_ID, request());

        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.hasNext()).isTrue();
        verify(placeTrackQueryRepository).findPlaceTracks(
                PLACE_ID,
                MEMBER_ID,
                PlaceTrackSort.POPULAR,
                PageRequest.of(0, 20)
        );
    }

    @Test
    void 최신순과_요청한_페이지_정보를_저장소에_전달한다() {
        PlaceTrackRequest.List request = new PlaceTrackRequest.List(
                PlaceTrackSort.LATEST,
                1,
                7,
                37.0,
                127.0
        );
        PageRequest pageable = PageRequest.of(1, 7);
        givenPlaceAndDistance(100.0);
        when(placeTrackQueryRepository.findPlaceTracks(
                PLACE_ID,
                MEMBER_ID,
                PlaceTrackSort.LATEST,
                pageable
        )).thenReturn(new SliceImpl<>(List.of(), pageable, false));

        placeTrackQueryService.getPlaceTracks(MEMBER_ID, PLACE_ID, request);

        verify(placeTrackQueryRepository).findPlaceTracks(
                PLACE_ID,
                MEMBER_ID,
                PlaceTrackSort.LATEST,
                PageRequest.of(1, 7)
        );
    }

    private Place givenPlaceAndDistance(double distance) {
        Place place = place();
        when(placeQueryService.getActivePlace(PLACE_ID)).thenReturn(place);
        when(pinLocationValidator.calculateDistance(
                37.0,
                127.0,
                place.getLocation().getY(),
                place.getLocation().getX()
        )).thenReturn(distance);
        return place;
    }

    private void givenNoLikedPlaceTrack() {
        when(placeTrackLikeRepository
                .existsByIdMemberIdAndPlaceTrackPlaceIdAndPlaceTrackDeletedAtIsNull(
                        MEMBER_ID,
                        PLACE_ID
                )).thenReturn(false);
    }

    private void givenTracks(List<PlaceTrackQueryResult> tracks, boolean hasNext) {
        when(placeTrackQueryRepository.findPlaceTracks(
                PLACE_ID,
                MEMBER_ID,
                PlaceTrackSort.POPULAR,
                PageRequest.of(0, 20)
        )).thenReturn(new SliceImpl<>(
                tracks,
                PageRequest.of(0, 20),
                hasNext
        ));
    }

    private PlaceTrackRequest.List request() {
        return new PlaceTrackRequest.List(
                PlaceTrackSort.POPULAR,
                0,
                20,
                37.0,
                127.0
        );
    }

    private PlaceTrackQueryResult track(Long id, int likeCount, boolean liked) {
        return new PlaceTrackQueryResult(
                id,
                "곡 " + id,
                "아티스트",
                "https://image.example/" + id,
                1,
                likeCount,
                liked
        );
    }

    private LikedPlaceTrackQueryResult likedTrack(
            Long id,
            String trackName,
            int likeCount
    ) {
        return new LikedPlaceTrackQueryResult(
                id,
                trackName,
                "아티스트",
                "https://image.example/" + id,
                likeCount
        );
    }

    private Place place() {
        Point location = new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(127.001, 37.001));
        Place place = Place.builder()
                .name("테스트 장소")
                .address("테스트 주소")
                .location(location)
                .build();
        ReflectionTestUtils.setField(place, "id", PLACE_ID);
        return place;
    }

    private PlaceTrack detailPlaceTrack() {
        Track track = mock(Track.class);
        when(track.getId()).thenReturn(20L);
        when(track.getProviderTrackId()).thenReturn("youtube-video-id");
        when(track.getTitle()).thenReturn("LOVE ATTACK");
        when(track.getArtistName()).thenReturn("RESCENE");
        when(track.getAlbumImageUrl())
                .thenReturn("https://example.com/love-attack.png");

        PlaceTrack placeTrack = mock(PlaceTrack.class);
        when(placeTrack.getId()).thenReturn(10L);
        when(placeTrack.getPlace()).thenReturn(place());
        when(placeTrack.getTrack()).thenReturn(track);
        when(placeTrack.getLikeCount()).thenReturn(33);
        return placeTrack;
    }

    private Place givenDetailPlaceTrack(double distance) {
        PlaceTrack placeTrack = detailPlaceTrack();
        Place place = placeTrack.getPlace();
        when(placeTrackRepository.findDetailByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(placeTrack));
        when(pinLocationValidator.calculateDistance(
                37.0,
                127.0,
                place.getLocation().getY(),
                place.getLocation().getX()
        )).thenReturn(distance);
        return place;
    }

    private PlaceTrackRequest.UserLocation userLocation() {
        return new PlaceTrackRequest.UserLocation(37.0, 127.0);
    }

    private void assertAccessDenied(String token) {
        assertThatThrownBy(() ->
                placeTrackQueryService.getPlaceTrackDetail(
                        MEMBER_ID,
                        10L,
                        userLocation(),
                        token
                ))
                .isInstanceOfSatisfying(TrackException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(TrackErrorCode.PLACE_TRACK_ACCESS_DENIED));

        verifyNoInteractions(placeTrackLikeRepository);
    }
}
