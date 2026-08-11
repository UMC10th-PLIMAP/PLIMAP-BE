package com.example.plimap.domain.track.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.plimap.domain.auth.service.command.impl.CustomOAuthService;
import com.example.plimap.domain.auth.service.command.impl.OAuthFailureHandler;
import com.example.plimap.domain.auth.service.command.impl.OAuthSuccessHandler;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.track.dto.request.PlaceTrackRequest;
import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;
import com.example.plimap.domain.track.enums.PlaceTrackSort;
import com.example.plimap.domain.track.exception.TrackErrorCode;
import com.example.plimap.domain.track.exception.TrackException;
import com.example.plimap.domain.track.exception.TrackSuccessCode;
import com.example.plimap.domain.track.service.command.PlaceTrackCommandService;
import com.example.plimap.domain.track.service.query.PlaceTrackQueryService;
import com.example.plimap.global.apiPayload.code.GeneralErrorCode;
import com.example.plimap.global.apiPayload.exception.GlobalExceptionHandler;
import com.example.plimap.global.config.CorsConfig;
import com.example.plimap.global.config.SecurityConfig;
import com.example.plimap.global.security.AuthCookieUtil;
import com.example.plimap.global.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.plimap.global.security.JwtUtil;
import com.example.plimap.global.security.SecurityErrorResponseHandler;
import com.example.plimap.global.security.TokenBlacklistService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.util.ReflectionTestUtils;

@WebMvcTest(controllers = PlaceTrackController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        SecurityErrorResponseHandler.class,
        AuthCookieUtil.class,
        HttpCookieOAuth2AuthorizationRequestRepository.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class PlaceTrackControllerTest {

    private static final String ENDPOINT = "/api/v1/places/1/tracks";
    private static final String DETAIL_ENDPOINT = "/api/v1/place-tracks/10";
    private static final String LIKE_ENDPOINT = "/api/v1/place-tracks/10/likes";
    private static final String LIKED_TRACKS_ENDPOINT = "/api/v1/place-tracks/likes";
    private static final String ACCESS_TOKEN = "valid-access-token";
    private static final String PLACE_ACCESS_TOKEN = "place-access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceTrackCommandService placeTrackCommandService;

    @MockitoBean
    private PlaceTrackQueryService placeTrackQueryService;

    @MockitoBean
    private CustomOAuthService customOAuthService;

    @MockitoBean
    private OAuthSuccessHandler oAuthSuccessHandler;

    @MockitoBean
    private OAuthFailureHandler oAuthFailureHandler;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private MemberCommandService memberCommandService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        when(jwtUtil.isValid(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.isAccessToken(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.getJti(ACCESS_TOKEN)).thenReturn("test-jti");
        when(tokenBlacklistService.isBlacklisted("test-jti")).thenReturn(false);
        when(jwtUtil.getMemberId(ACCESS_TOKEN)).thenReturn(1L);
        Member member = Member.builder().nickname("사용자").build();
        ReflectionTestUtils.setField(member, "id", 1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
    }

    @Test
    void 기본_요청은_장소별_곡_목록을_반환한다() throws Exception {
        when(placeTrackQueryService.getPlaceTracks(any(), any(), any()))
                .thenReturn(response());

        mockMvc.perform(authenticatedRequest()
                        .queryParam("latitude", "37.5665")
                        .queryParam("longitude", "126.9780"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("PLACE_TRACK_LIST_SUCCESS"))
                .andExpect(jsonPath("$.message")
                        .value("장소별 곡 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.result.placeId").value(1))
                .andExpect(jsonPath("$.result.placeName").doesNotExist())
                .andExpect(jsonPath("$.result.createdBy").doesNotExist())
                .andExpect(jsonPath("$.result.isBookmarked").doesNotExist())
                .andExpect(jsonPath("$.result.isWithinRadius").value(true))
                .andExpect(jsonPath("$.result.isTrackDetailAccessible").value(true))
                .andExpect(jsonPath("$.result.tracks[0].placeTrackId").value(10))
                .andExpect(jsonPath("$.result.tracks[0].pinCount").value(1))
                .andExpect(jsonPath("$.result.tracks[0].likeCount").value(5))
                .andExpect(jsonPath("$.result.tracks[0].pinByMe").value(true));

        verify(placeTrackQueryService).getPlaceTracks(
                1L,
                1L,
                new PlaceTrackRequest.List(
                        PlaceTrackSort.POPULAR,
                        0,
                        20,
                        37.5665,
                        126.9780
                )
        );
    }

    @Test
    void 장소_노래_상세의_모든_필드를_반환한다() throws Exception {
        PlaceTrackRequest.UserLocation request =
                new PlaceTrackRequest.UserLocation(37.5665, 126.9780);
        when(placeTrackQueryService.getPlaceTrackDetail(
                1L,
                10L,
                request,
                PLACE_ACCESS_TOKEN
        ))
                .thenReturn(detailResponse());

        mockMvc.perform(authenticatedDetailRequest(DETAIL_ENDPOINT)
                        .header("Place-Access-Token", PLACE_ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value(
                        TrackSuccessCode.PLACE_TRACK_DETAIL_SUCCESS.getCode()))
                .andExpect(jsonPath("$.message")
                        .value(TrackSuccessCode
                                .PLACE_TRACK_DETAIL_SUCCESS
                                .getMessage()))
                .andExpect(jsonPath("$.result.placeTrackId").value(10))
                .andExpect(jsonPath("$.result.trackId").value(20))
                .andExpect(jsonPath("$.result.youtubeVideoId")
                        .value("youtube-video-id"))
                .andExpect(jsonPath("$.result.title").value("LOVE ATTACK"))
                .andExpect(jsonPath("$.result.artist").value("RESCENE"))
                .andExpect(jsonPath("$.result.albumImageUrl")
                        .value("https://example.com/love-attack.png"))
                .andExpect(jsonPath("$.result.likeCount").value(33))
                .andExpect(jsonPath("$.result.userLike").value(true));

        verify(placeTrackQueryService).getPlaceTrackDetail(
                1L,
                10L,
                request,
                PLACE_ACCESS_TOKEN
        );
    }

    @Test
    void 장소_노래_ID_타입이_잘못되면_400을_반환한다() throws Exception {
        mockMvc.perform(authenticatedDetailRequest(
                        "/api/v1/place-tracks/not-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code")
                        .value(GeneralErrorCode.TYPE_MISMATCH.getCode()))
                .andExpect(jsonPath("$.result").isEmpty());

        verifyNoInteractions(placeTrackQueryService);
    }

    @Test
    void 장소_노래_ID가_0이면_400을_반환한다() throws Exception {
        mockMvc.perform(authenticatedDetailRequest("/api/v1/place-tracks/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value(GeneralErrorCode.VALIDATION_FAILED.getCode()));

        verifyNoInteractions(placeTrackQueryService);
    }

    @Test
    void 장소_노래_ID가_음수이면_400을_반환한다() throws Exception {
        mockMvc.perform(authenticatedDetailRequest("/api/v1/place-tracks/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value(GeneralErrorCode.VALIDATION_FAILED.getCode()));

        verifyNoInteractions(placeTrackQueryService);
    }

    @Test
    void 장소_노래_상세_인증이_누락되면_401을_반환한다() throws Exception {
        mockMvc.perform(get(DETAIL_ENDPOINT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code")
                        .value(GeneralErrorCode.UNAUTHORIZED.getCode()))
                .andExpect(jsonPath("$.result").isEmpty());

        verifyNoInteractions(placeTrackQueryService);
    }

    @Test
    void 장소_노래가_없으면_PLACE_TRACK_NOT_FOUND를_반환한다() throws Exception {
        when(placeTrackQueryService.getPlaceTrackDetail(
                1L,
                10L,
                new PlaceTrackRequest.UserLocation(37.5665, 126.9780),
                null
        ))
                .thenThrow(new TrackException(
                        TrackErrorCode.PLACE_TRACK_NOT_FOUND
                ));

        mockMvc.perform(authenticatedDetailRequest(DETAIL_ENDPOINT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code")
                        .value(TrackErrorCode.PLACE_TRACK_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message")
                        .value(TrackErrorCode
                                .PLACE_TRACK_NOT_FOUND
                                .getMessage()))
                .andExpect(jsonPath("$.result").isEmpty());
    }

    @Test
    void 장소_노래_상세_접근_권한이_없으면_403을_반환한다() throws Exception {
        when(placeTrackQueryService.getPlaceTrackDetail(
                1L,
                10L,
                new PlaceTrackRequest.UserLocation(37.5665, 126.9780),
                null
        )).thenThrow(new TrackException(
                TrackErrorCode.PLACE_TRACK_ACCESS_DENIED
        ));

        mockMvc.perform(authenticatedDetailRequest(DETAIL_ENDPOINT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(
                        TrackErrorCode.PLACE_TRACK_ACCESS_DENIED.getCode()))
                .andExpect(jsonPath("$.message").value(
                        TrackErrorCode.PLACE_TRACK_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.result").isEmpty());
    }

    @Test
    void 장소_노래_상세의_위도가_누락되면_400을_반환한다() throws Exception {
        mockMvc.perform(authenticatedGet(DETAIL_ENDPOINT)
                        .queryParam("userLongitude", "126.9780"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(placeTrackQueryService);
    }

    @Test
    void 장소_노래_상세의_경도가_누락되면_400을_반환한다() throws Exception {
        mockMvc.perform(authenticatedGet(DETAIL_ENDPOINT)
                        .queryParam("userLatitude", "37.5665"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(placeTrackQueryService);
    }

    @Test
    void 장소별_곡_좋아요를_등록하면_변경된_상태와_개수를_반환한다() throws Exception {
        when(placeTrackCommandService.createPlaceTrackLike(1L, 10L))
                .thenReturn(new PlaceTrackResponse.PlaceTrackLikeResult(
                        10L,
                        true,
                        13
                ));

        mockMvc.perform(authenticatedPut(LIKE_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value(
                        TrackSuccessCode.PLACE_TRACK_LIKE_PUT_SUCCESS.getCode()))
                .andExpect(jsonPath("$.message").value(
                        TrackSuccessCode.PLACE_TRACK_LIKE_PUT_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.result.placeTrackId").value(10))
                .andExpect(jsonPath("$.result.isLiked").value(true))
                .andExpect(jsonPath("$.result.likeCount").value(13));

        verify(placeTrackCommandService).createPlaceTrackLike(1L, 10L);
    }

    @Test
    void 장소별_곡_좋아요를_삭제하면_변경된_상태와_개수를_반환한다() throws Exception {
        when(placeTrackCommandService.deletePlaceTrackLike(1L, 10L))
                .thenReturn(new PlaceTrackResponse.PlaceTrackLikeResult(
                        10L,
                        false,
                        12
                ));

        mockMvc.perform(authenticatedDelete(LIKE_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value(
                        TrackSuccessCode.PLACE_TRACK_LIKE_DELETE_SUCCESS.getCode()))
                .andExpect(jsonPath("$.message").value(
                        TrackSuccessCode.PLACE_TRACK_LIKE_DELETE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.result.placeTrackId").value(10))
                .andExpect(jsonPath("$.result.isLiked").value(false))
                .andExpect(jsonPath("$.result.likeCount").value(12));

        verify(placeTrackCommandService).deletePlaceTrackLike(1L, 10L);
    }

    @Test
    void 좋아요한_장소별_곡_목록을_반환한다() throws Exception {
        when(placeTrackQueryService.getLikedPlaceTracks(1L, 0, 20))
                .thenReturn(likedListResponse());

        mockMvc.perform(authenticatedGet(LIKED_TRACKS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value(
                        TrackSuccessCode.LIKED_PLACE_TRACK_LIST_SUCCESS.getCode()))
                .andExpect(jsonPath("$.message")
                        .value("좋아요한 곡 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.result.tracks[0].placeTrackId").value(10))
                .andExpect(jsonPath("$.result.tracks[0].trackName")
                        .value("LOVE ATTACK"))
                .andExpect(jsonPath("$.result.tracks[0].artistName")
                        .value("RESCENE"))
                .andExpect(jsonPath("$.result.tracks[0].artworkUrl")
                        .value("https://image.example/love-attack.jpg"))
                .andExpect(jsonPath("$.result.tracks[0].likeCount").value(5))
                .andExpect(jsonPath("$.result.tracks[0].isLiked").doesNotExist())
                .andExpect(jsonPath("$.result.tracks[0].pinCount").doesNotExist())
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(20))
                .andExpect(jsonPath("$.result.hasNext").value(false));

        verify(placeTrackQueryService).getLikedPlaceTracks(1L, 0, 20);
    }

    @Test
    void 좋아요한_장소별_곡이_없으면_빈_배열을_반환한다() throws Exception {
        when(placeTrackQueryService.getLikedPlaceTracks(1L, 0, 20))
                .thenReturn(new PlaceTrackResponse.LikedPlaceTrackListResult(
                        List.of(),
                        0,
                        20,
                        false
                ));

        mockMvc.perform(authenticatedGet(LIKED_TRACKS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tracks").isArray())
                .andExpect(jsonPath("$.result.tracks").isEmpty())
                .andExpect(jsonPath("$.result.hasNext").value(false));
    }

    @Test
    void 좋아요한_장소별_곡_목록의_페이지_조건을_서비스에_전달한다()
            throws Exception {
        when(placeTrackQueryService.getLikedPlaceTracks(1L, 2, 30))
                .thenReturn(new PlaceTrackResponse.LikedPlaceTrackListResult(
                        List.of(),
                        2,
                        30,
                        false
                ));

        mockMvc.perform(authenticatedGet(LIKED_TRACKS_ENDPOINT)
                        .queryParam("page", "2")
                        .queryParam("size", "30"))
                .andExpect(status().isOk());

        verify(placeTrackQueryService).getLikedPlaceTracks(1L, 2, 30);
    }

    @Test
    void 인증되지_않은_좋아요한_장소별_곡_목록_조회는_401을_반환한다()
            throws Exception {
        mockMvc.perform(get(LIKED_TRACKS_ENDPOINT))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(placeTrackQueryService);
    }

    @Test
    void 좋아요한_장소별_곡_목록의_page가_음수이면_400을_반환한다()
            throws Exception {
        mockMvc.perform(authenticatedGet(LIKED_TRACKS_ENDPOINT)
                        .queryParam("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value(GeneralErrorCode.VALIDATION_FAILED.getCode()));

        verifyNoInteractions(placeTrackQueryService);
    }

    @Test
    void 좋아요한_장소별_곡_목록의_size가_범위를_벗어나면_400을_반환한다()
            throws Exception {
        mockMvc.perform(authenticatedGet(LIKED_TRACKS_ENDPOINT)
                        .queryParam("size", "201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value(GeneralErrorCode.VALIDATION_FAILED.getCode()));

        verifyNoInteractions(placeTrackQueryService);
    }

    @Test
    void 인증되지_않은_장소별_곡_좋아요_등록은_401을_반환한다() throws Exception {
        mockMvc.perform(put(LIKE_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(placeTrackCommandService);
    }

    @Test
    void 인증되지_않은_장소별_곡_좋아요_삭제는_401을_반환한다() throws Exception {
        mockMvc.perform(delete(LIKE_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(placeTrackCommandService);
    }

    @Test
    void 좋아요_등록시_placeTrackId가_0이면_validation_오류를_반환한다() throws Exception {
        assertLikeValidationError(authenticatedPut(
                "/api/v1/place-tracks/0/likes"
        ));
    }

    @Test
    void 좋아요_등록시_placeTrackId가_음수이면_validation_오류를_반환한다() throws Exception {
        assertLikeValidationError(authenticatedPut(
                "/api/v1/place-tracks/-1/likes"
        ));
    }

    @Test
    void 좋아요_삭제시_placeTrackId가_0이면_validation_오류를_반환한다() throws Exception {
        assertLikeValidationError(authenticatedDelete(
                "/api/v1/place-tracks/0/likes"
        ));
    }

    @Test
    void 좋아요_삭제시_placeTrackId가_음수이면_validation_오류를_반환한다() throws Exception {
        assertLikeValidationError(authenticatedDelete(
                "/api/v1/place-tracks/-1/likes"
        ));
    }

    @Test
    void 좋아요_등록시_placeTrackId가_비숫자이면_타입_불일치_오류를_반환한다()
            throws Exception {
        assertLikeTypeMismatch(authenticatedPut(
                "/api/v1/place-tracks/not-number/likes"
        ));
    }

    @Test
    void 좋아요_삭제시_placeTrackId가_비숫자이면_타입_불일치_오류를_반환한다()
            throws Exception {
        assertLikeTypeMismatch(authenticatedDelete(
                "/api/v1/place-tracks/not-number/likes"
        ));
    }

    @Test
    void 정렬_페이지_크기와_좌표를_서비스에_전달한다() throws Exception {
        when(placeTrackQueryService.getPlaceTracks(any(), any(), any()))
                .thenReturn(response());

        mockMvc.perform(authenticatedRequest()
                        .queryParam("sort", "LATEST")
                        .queryParam("page", "2")
                        .queryParam("size", "30")
                        .queryParam("latitude", "37.1")
                        .queryParam("longitude", "127.2"))
                .andExpect(status().isOk());

        verify(placeTrackQueryService).getPlaceTracks(
                1L,
                1L,
                new PlaceTrackRequest.List(
                        PlaceTrackSort.LATEST,
                        2,
                        30,
                        37.1,
                        127.2
                )
        );
    }

    @Test
    void 인증이_누락되면_401을_반환한다() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .queryParam("latitude", "37.0")
                        .queryParam("longitude", "127.0"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(placeTrackQueryService);
    }

    @Test
    void page가_음수이면_검증에_실패한다() throws Exception {
        assertBadRequest(authenticatedRequest()
                .queryParam("page", "-1")
                .queryParam("latitude", "37.0")
                .queryParam("longitude", "127.0"));
    }

    @Test
    void size가_0이면_검증에_실패한다() throws Exception {
        assertBadRequest(authenticatedRequest()
                .queryParam("size", "0")
                .queryParam("latitude", "37.0")
                .queryParam("longitude", "127.0"));
    }

    @Test
    void size가_200을_초과하면_검증에_실패한다() throws Exception {
        assertBadRequest(authenticatedRequest()
                .queryParam("size", "201")
                .queryParam("latitude", "37.0")
                .queryParam("longitude", "127.0"));
    }

    @Test
    void 지원하지_않는_sort이면_400을_반환한다() throws Exception {
        assertBadRequest(authenticatedRequest()
                .queryParam("sort", "OLDEST")
                .queryParam("latitude", "37.0")
                .queryParam("longitude", "127.0"));
    }

    @Test
    void 위도가_누락되면_400을_반환한다() throws Exception {
        assertBadRequest(authenticatedRequest().queryParam("longitude", "127.0"));
    }

    @Test
    void 경도가_누락되면_400을_반환한다() throws Exception {
        assertBadRequest(authenticatedRequest().queryParam("latitude", "37.0"));
    }

    @Test
    void 위도가_범위를_벗어나면_400을_반환한다() throws Exception {
        assertBadRequest(authenticatedRequest()
                .queryParam("latitude", "90.1")
                .queryParam("longitude", "127.0"));
    }

    @Test
    void 경도가_범위를_벗어나면_400을_반환한다() throws Exception {
        assertBadRequest(authenticatedRequest()
                .queryParam("latitude", "37.0")
                .queryParam("longitude", "-180.1"));
    }

    @Test
    void 위도가_NaN이면_400을_반환한다() throws Exception {
        assertBadRequest(authenticatedRequest()
                .queryParam("latitude", "NaN")
                .queryParam("longitude", "127.0"));
    }

    @Test
    void 경도가_NaN이면_400을_반환한다() throws Exception {
        assertBadRequest(authenticatedRequest()
                .queryParam("latitude", "37.0")
                .queryParam("longitude", "NaN"));
    }

    private void assertBadRequest(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isBadRequest());
        verifyNoInteractions(placeTrackQueryService);
    }

    private MockHttpServletRequestBuilder authenticatedRequest() {
        return authenticatedGet(ENDPOINT);
    }

    private MockHttpServletRequestBuilder authenticatedGet(String endpoint) {
        return get(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN);
    }

    private MockHttpServletRequestBuilder authenticatedDetailRequest(String endpoint) {
        return authenticatedGet(endpoint)
                .queryParam("userLatitude", "37.5665")
                .queryParam("userLongitude", "126.9780");
    }

    private MockHttpServletRequestBuilder authenticatedPut(String endpoint) {
        return put(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN);
    }

    private MockHttpServletRequestBuilder authenticatedDelete(String endpoint) {
        return delete(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN);
    }

    private void assertLikeValidationError(MockHttpServletRequestBuilder request)
            throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value(GeneralErrorCode.VALIDATION_FAILED.getCode()));
        verifyNoInteractions(placeTrackCommandService);
    }

    private void assertLikeTypeMismatch(MockHttpServletRequestBuilder request)
            throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value(GeneralErrorCode.TYPE_MISMATCH.getCode()));
        verifyNoInteractions(placeTrackCommandService);
    }

    private PlaceTrackResponse.PlaceTrackListResult response() {
        return new PlaceTrackResponse.PlaceTrackListResult(
                1L,
                100.0,
                true,
                true,
                List.of(new PlaceTrackResponse.PlaceTrackItem(
                        10L,
                        "LOVE ATTACK",
                        "RESCENE",
                        "https://image.example/artwork.jpg",
                        1,
                        5,
                        true,
                        true
                )),
                0,
                20,
                false
        );
    }

    private PlaceTrackResponse.PlaceTrackDetail detailResponse() {
        return new PlaceTrackResponse.PlaceTrackDetail(
                10L,
                20L,
                "youtube-video-id",
                "LOVE ATTACK",
                "RESCENE",
                "https://example.com/love-attack.png",
                33,
                true
        );
    }

    private PlaceTrackResponse.LikedPlaceTrackListResult likedListResponse() {
        return new PlaceTrackResponse.LikedPlaceTrackListResult(
                List.of(new PlaceTrackResponse.LikedPlaceTrackItem(
                        10L,
                        "LOVE ATTACK",
                        "RESCENE",
                        "https://image.example/love-attack.jpg",
                        5
                )),
                0,
                20,
                false
        );
    }
}
