package com.example.plimap.domain.track.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.plimap.domain.auth.service.command.impl.CustomOAuthService;
import com.example.plimap.domain.auth.service.command.impl.OAuthFailureHandler;
import com.example.plimap.domain.auth.service.command.impl.OAuthSuccessHandler;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.track.dto.request.PlaceTrackRequest;
import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;
import com.example.plimap.domain.track.enums.PlaceTrackSort;
import com.example.plimap.domain.track.service.query.PlaceTrackQueryService;
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
    private static final String ACCESS_TOKEN = "valid-access-token";

    @Autowired
    private MockMvc mockMvc;

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
                .andExpect(jsonPath("$.code").value("PLACE_TRACK200_1"))
                .andExpect(jsonPath("$.message")
                        .value("장소별 곡 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.result.placeId").value(1))
                .andExpect(jsonPath("$.result.placeName").doesNotExist())
                .andExpect(jsonPath("$.result.createdBy").doesNotExist())
                .andExpect(jsonPath("$.result.isBookmarked").doesNotExist())
                .andExpect(jsonPath("$.result.isWithinRadius").value(true))
                .andExpect(jsonPath("$.result.tracks[0].placeTrackId").value(10))
                .andExpect(jsonPath("$.result.tracks[0].pinCount").value(1))
                .andExpect(jsonPath("$.result.tracks[0].likeCount").value(5));

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
        return get(ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN);
    }

    private PlaceTrackResponse.PlaceTrackListResult response() {
        return new PlaceTrackResponse.PlaceTrackListResult(
                1L,
                100.0,
                true,
                List.of(new PlaceTrackResponse.PlaceTrackItem(
                        10L,
                        "LOVE ATTACK",
                        "RESCENE",
                        "https://image.example/artwork.jpg",
                        1,
                        5,
                        true
                )),
                0,
                20,
                false
        );
    }
}
