package com.example.plimap.domain.home.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.plimap.domain.auth.service.command.impl.CustomOAuthService;
import com.example.plimap.domain.auth.service.command.impl.OAuthFailureHandler;
import com.example.plimap.domain.auth.service.command.impl.OAuthSuccessHandler;
import com.example.plimap.domain.home.dto.response.HomeResponse;
import com.example.plimap.domain.home.service.query.HomeQueryService;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberRole;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.global.apiPayload.exception.GlobalExceptionHandler;
import com.example.plimap.global.config.CorsConfig;
import com.example.plimap.global.config.SecurityConfig;
import com.example.plimap.global.security.AuthCookieUtil;
import com.example.plimap.global.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.plimap.global.security.JwtUtil;
import com.example.plimap.global.security.SecurityErrorResponseHandler;
import com.example.plimap.global.security.TokenBlacklistService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HomeController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        SecurityErrorResponseHandler.class,
        AuthCookieUtil.class,
        HttpCookieOAuth2AuthorizationRequestRepository.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class HomeControllerTest {

    private static final String ENDPOINT = "/api/v1/home/context";
    private static final String ACCESS_TOKEN = "valid-access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomeQueryService homeQueryService;

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
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(member.getNickname()).thenReturn("델리만쥬");
        when(member.getRole()).thenReturn(MemberRole.USER);
        when(member.getStatus()).thenReturn(MemberStatus.ACTIVE);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
    }

    @Test
    void 홈_컨텍스트_조회에_성공한다() throws Exception {
        when(homeQueryService.getHomeContext("델리만쥬", 37.5, 127.03))
                .thenReturn(context());

        mockMvc.perform(authenticatedRequest()
                        .param("latitude", "37.5")
                        .param("longitude", "127.03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("HOME_200_CONTEXT_FETCHED"))
                .andExpect(jsonPath("$.message").value("홈 컨텍스트를 조회했습니다."))
                .andExpect(jsonPath("$.result.nickname").value("델리만쥬"))
                .andExpect(jsonPath("$.result.currentRegion.sido").value("서울특별시"))
                .andExpect(jsonPath("$.result.currentRegion.sigungu").value("강남구"))
                .andExpect(jsonPath("$.result.currentRegion.eupMyeonDong").value("역삼1동"))
                .andExpect(jsonPath("$.result.currentRegion.displayName")
                        .value("서울특별시 강남구"));

        verify(homeQueryService).getHomeContext("델리만쥬", 37.5, 127.03);
    }

    @Test
    void 행정동_결과가_없어도_currentRegion_객체와_null_필드를_반환한다() throws Exception {
        when(homeQueryService.getHomeContext("델리만쥬", 37.5, 127.03))
                .thenReturn(new HomeResponse.Context(
                        "델리만쥬",
                        new HomeResponse.CurrentRegion(null, null, null, null)
                ));

        mockMvc.perform(authenticatedRequest()
                        .param("latitude", "37.5")
                        .param("longitude", "127.03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.currentRegion").exists())
                .andExpect(jsonPath("$.result.currentRegion.sido").value(nullValue()))
                .andExpect(jsonPath("$.result.currentRegion.sigungu").value(nullValue()))
                .andExpect(jsonPath("$.result.currentRegion.eupMyeonDong").value(nullValue()))
                .andExpect(jsonPath("$.result.currentRegion.displayName").value(nullValue()));
    }

    @Test
    void 닉네임이_null이면_null로_반환한다() throws Exception {
        when(homeQueryService.getHomeContext("델리만쥬", 37.5, 127.03))
                .thenReturn(new HomeResponse.Context(
                        null,
                        new HomeResponse.CurrentRegion(null, null, null, null)
                ));

        mockMvc.perform(authenticatedRequest()
                        .param("latitude", "37.5")
                        .param("longitude", "127.03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.nickname").value(nullValue()));
    }

    @Test
    void 인증이_누락되면_공통_401을_반환한다() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("latitude", "37.5")
                        .param("longitude", "127.03"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON_401_UNAUTHORIZED"));

        verifyNoInteractions(homeQueryService);
    }

    @Test
    void latitude가_누락되면_공통_validation_400을_반환한다() throws Exception {
        mockMvc.perform(authenticatedRequest().param("longitude", "127.03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"));

        verifyNoInteractions(homeQueryService);
    }

    @Test
    void longitude가_누락되면_공통_validation_400을_반환한다() throws Exception {
        mockMvc.perform(authenticatedRequest().param("latitude", "37.5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"));

        verifyNoInteractions(homeQueryService);
    }

    @ParameterizedTest
    @CsvSource({
            "-90, 127.03",
            "90, 127.03",
            "37.5, -180",
            "37.5, 180"
    })
    void 좌표_경계값을_허용한다(double latitude, double longitude) throws Exception {
        when(homeQueryService.getHomeContext("델리만쥬", latitude, longitude))
                .thenReturn(context());

        mockMvc.perform(authenticatedRequest()
                        .param("latitude", Double.toString(latitude))
                        .param("longitude", Double.toString(longitude)))
                .andExpect(status().isOk());

        verify(homeQueryService).getHomeContext("델리만쥬", latitude, longitude);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-90.1", "90.1"})
    void 위도_범위를_벗어나면_공통_validation_400을_반환한다(String latitude)
            throws Exception {
        mockMvc.perform(authenticatedRequest()
                        .param("latitude", latitude)
                        .param("longitude", "127.03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"));

        verifyNoInteractions(homeQueryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-180.1", "180.1"})
    void 경도_범위를_벗어나면_공통_validation_400을_반환한다(String longitude)
            throws Exception {
        mockMvc.perform(authenticatedRequest()
                        .param("latitude", "37.5")
                        .param("longitude", longitude))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"));

        verifyNoInteractions(homeQueryService);
    }

    @Test
    void Place_외부_API_오류를_502로_반환한다() throws Exception {
        when(homeQueryService.getHomeContext("델리만쥬", 37.5, 127.03))
                .thenThrow(new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR));

        mockMvc.perform(authenticatedRequest()
                        .param("latitude", "37.5")
                        .param("longitude", "127.03"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PLACE_EXTERNAL_API_ERROR"));
    }

    @Test
    void Place_외부_API_timeout을_504로_반환한다() throws Exception {
        when(homeQueryService.getHomeContext("델리만쥬", 37.5, 127.03))
                .thenThrow(new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_TIMEOUT));

        mockMvc.perform(authenticatedRequest()
                        .param("latitude", "37.5")
                        .param("longitude", "127.03"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("PLACE_EXTERNAL_API_TIMEOUT"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            authenticatedRequest() {
        return get(ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN);
    }

    private HomeResponse.Context context() {
        return new HomeResponse.Context(
                "델리만쥬",
                new HomeResponse.CurrentRegion(
                        "서울특별시",
                        "강남구",
                        "역삼1동",
                        "서울특별시 강남구"
                )
        );
    }
}
