package com.example.plimap.domain.place.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.plimap.domain.auth.service.command.impl.CustomOAuthService;
import com.example.plimap.domain.auth.service.command.impl.OAuthFailureHandler;
import com.example.plimap.domain.auth.service.command.impl.OAuthSuccessHandler;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.service.command.PlaceCommandService;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
import com.example.plimap.global.apiPayload.exception.GlobalExceptionHandler;
import com.example.plimap.global.config.CorsConfig;
import com.example.plimap.global.config.SecurityConfig;
import com.example.plimap.global.security.AuthCookieUtil;
import com.example.plimap.global.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.plimap.global.security.JwtUtil;
import com.example.plimap.global.security.SecurityErrorResponseHandler;
import com.example.plimap.global.security.TokenBlacklistService;
import java.time.Instant;
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

@WebMvcTest(controllers = PlaceController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        SecurityErrorResponseHandler.class,
        AuthCookieUtil.class,
        HttpCookieOAuth2AuthorizationRequestRepository.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class PlaceSearchHistoryControllerTest {

    private static final String ENDPOINT = "/api/v1/places/search-histories";
    private static final String ACCESS_TOKEN = "valid-access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceCommandService placeCommandService;

    @MockitoBean
    private PlaceQueryService placeQueryService;

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
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
    }

    @Test
    void 최근_검색_장소_목록_조회에_성공하면_명세_응답을_반환한다() throws Exception {
        when(placeQueryService.getSearchHistories(1L, 37.5283, 126.9326))
                .thenReturn(new PlaceResponse.SearchHistoryResult(List.of(
                        new PlaceResponse.SearchHistoryItem(
                                10L,
                                1L,
                                "한강",
                                "공원",
                                "서울특별시 영등포구 여의도동",
                                "서울특별시 영등포구 여의동로",
                                37.5283,
                                126.9326,
                                470,
                                true,
                                "홍길동",
                                Instant.parse("2026-07-05T12:30:00Z")
                        )
                )));

        mockMvc.perform(authenticatedGet()
                        .param("latitude", "37.5283")
                        .param("longitude", "126.9326"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code")
                        .value("PLACE_SEARCH_HISTORY_LIST_SUCCESS"))
                .andExpect(jsonPath("$.message")
                        .value("최근 검색 장소 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.result.items[0].historyId").value(10))
                .andExpect(jsonPath("$.result.items[0].placeId").value(1))
                .andExpect(jsonPath("$.result.items[0].placeName").value("한강"))
                .andExpect(jsonPath("$.result.items[0].category").value("공원"))
                .andExpect(jsonPath("$.result.items[0].address")
                        .value("서울특별시 영등포구 여의도동"))
                .andExpect(jsonPath("$.result.items[0].roadAddress")
                        .value("서울특별시 영등포구 여의동로"))
                .andExpect(jsonPath("$.result.items[0].latitude").value(37.5283))
                .andExpect(jsonPath("$.result.items[0].longitude").value(126.9326))
                .andExpect(jsonPath("$.result.items[0].distanceMeters").value(470))
                .andExpect(jsonPath("$.result.items[0].hasPin").value(true))
                .andExpect(jsonPath("$.result.items[0].firstPinCreatorNickname")
                        .value("홍길동"))
                .andExpect(jsonPath("$.result.items[0].selectedAt")
                        .value("2026-07-05T12:30:00Z"));
    }

    @Test
    void 조회_결과가_없으면_빈_items를_반환한다() throws Exception {
        when(placeQueryService.getSearchHistories(1L, 37.5283, 126.9326))
                .thenReturn(new PlaceResponse.SearchHistoryResult(List.of()));

        mockMvc.perform(authenticatedGet()
                        .param("latitude", "37.5283")
                        .param("longitude", "126.9326"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isArray())
                .andExpect(jsonPath("$.result.items").isEmpty());
    }

    @Test
    void 위도가_누락되면_공통_검증_400을_반환한다() throws Exception {
        mockMvc.perform(authenticatedGet()
                        .param("longitude", "126.9326"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("위치 정보가 올바르지 않습니다."));

        verifyNoInteractions(placeQueryService);
    }

    @Test
    void 경도가_유효_범위를_벗어나면_공통_검증_400을_반환한다() throws Exception {
        mockMvc.perform(authenticatedGet()
                        .param("latitude", "37.5283")
                        .param("longitude", "181"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("위치 정보가 올바르지 않습니다."));

        verifyNoInteractions(placeQueryService);
    }

    @Test
    void 최근_검색_이력_삭제에_성공하면_null_결과를_반환한다() throws Exception {
        mockMvc.perform(delete(ENDPOINT + "/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code")
                        .value("PLACE_SEARCH_HISTORY_DELETE_SUCCESS"))
                .andExpect(jsonPath("$.message")
                        .value("최근 검색 장소 삭제에 성공했습니다."))
                .andExpect(jsonPath("$.result").value(nullValue()));

        verify(placeCommandService).deleteSearchHistory(1L, 10L);
    }

    @Test
    void 이력이_없거나_다른_사용자가_소유하면_동일한_404를_반환한다() throws Exception {
        doThrow(new PlaceException(PlaceErrorCode.PLACE_SEARCH_HISTORY_NOT_FOUND))
                .when(placeCommandService)
                .deleteSearchHistory(1L, 10L);

        mockMvc.perform(delete(ENDPOINT + "/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("PLACE_SEARCH_HISTORY_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("최근 검색 이력을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.result").value(nullValue()));
    }

    @Test
    void 인증되지_않은_목록_조회와_삭제는_공통_401을_반환한다() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("latitude", "37.5283")
                        .param("longitude", "126.9326"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON_401_UNAUTHORIZED"));

        mockMvc.perform(delete(ENDPOINT + "/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON_401_UNAUTHORIZED"));

        verifyNoInteractions(placeQueryService, placeCommandService);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            authenticatedGet() {
        return get(ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN);
    }
}
