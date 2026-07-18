package com.example.plimap.domain.track.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.plimap.domain.auth.service.command.impl.CustomOAuthService;
import com.example.plimap.domain.auth.service.command.impl.OAuthSuccessHandler;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.track.dto.request.TrackRequest;
import com.example.plimap.domain.track.dto.response.TrackResponse;
import com.example.plimap.domain.track.exception.TrackErrorCode;
import com.example.plimap.domain.track.exception.TrackException;
import com.example.plimap.domain.track.service.query.TrackQueryService;
import com.example.plimap.global.apiPayload.exception.GlobalExceptionHandler;
import com.example.plimap.global.config.CorsConfig;
import com.example.plimap.global.config.SecurityConfig;
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

@WebMvcTest(controllers = TrackController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        SecurityErrorResponseHandler.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class TrackControllerTest {

    private static final String ENDPOINT = "/api/v1/tracks/search";
    private static final String ACCESS_TOKEN = "valid-access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrackQueryService trackQueryService;

    @MockitoBean
    private CustomOAuthService customOAuthService;

    @MockitoBean
    private OAuthSuccessHandler oAuthSuccessHandler;

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
        when(memberRepository.findById(1L)).thenReturn(Optional.of(Member.builder().build()));
    }

    @Test
    void 인증된_사용자가_검색하면_TRACK_SEARCH_SUCCESS를_반환한다() throws Exception {
        when(trackQueryService.searchTracks(any())).thenReturn(searchResult());

        mockMvc.perform(get(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                        .queryParam("keyword", "아이유"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("TRACK_SEARCH_SUCCESS"))
                .andExpect(jsonPath("$.message").value("음악 검색에 성공했습니다."))
                .andExpect(jsonPath("$.result.tracks[0].itunesTrackId").value(123))
                .andExpect(jsonPath("$.result.tracks[0].trackName").value("밤편지"))
                .andExpect(jsonPath("$.result.tracks[0].durationMs").value(253000));

        verify(trackQueryService).searchTracks(new TrackRequest.Search("아이유", 20));
    }

    @Test
    void limit을_생략하면_기본값_20을_사용한다() throws Exception {
        when(trackQueryService.searchTracks(any())).thenReturn(searchResult());

        mockMvc.perform(authenticatedSearch().queryParam("keyword", "아이유"))
                .andExpect(status().isOk());

        verify(trackQueryService).searchTracks(new TrackRequest.Search("아이유", 20));
    }

    @Test
    void limit_최솟값_1을_허용한다() throws Exception {
        when(trackQueryService.searchTracks(any())).thenReturn(searchResult());

        mockMvc.perform(authenticatedSearch()
                        .queryParam("keyword", "아이유")
                        .queryParam("limit", "1"))
                .andExpect(status().isOk());

        verify(trackQueryService).searchTracks(new TrackRequest.Search("아이유", 1));
    }

    @Test
    void limit_최댓값_200을_허용한다() throws Exception {
        when(trackQueryService.searchTracks(any())).thenReturn(searchResult());

        mockMvc.perform(authenticatedSearch()
                        .queryParam("keyword", "아이유")
                        .queryParam("limit", "200"))
                .andExpect(status().isOk());

        verify(trackQueryService).searchTracks(new TrackRequest.Search("아이유", 200));
    }

    @Test
    void keyword가_누락되면_COMMON_400_MISSING_PARAMETER를_반환한다() throws Exception {
        mockMvc.perform(authenticatedSearch())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_MISSING_PARAMETER"))
                .andExpect(jsonPath("$.result").isEmpty());

        verifyNoInteractions(trackQueryService);
    }

    @Test
    void keyword가_blank이면_COMMON_400_VALIDATION_FAILED를_반환한다() throws Exception {
        mockMvc.perform(authenticatedSearch().queryParam("keyword", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("검색어를 입력해주세요."))
                .andExpect(jsonPath("$.result").isEmpty());

        verifyNoInteractions(trackQueryService);
    }

    @Test
    void limit이_허용_범위를_벗어나면_COMMON_400_VALIDATION_FAILED를_반환한다() throws Exception {
        mockMvc.perform(authenticatedSearch()
                        .queryParam("keyword", "아이유")
                        .queryParam("limit", "201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.result").isEmpty());

        verifyNoInteractions(trackQueryService);
    }

    @Test
    void limit이_1보다_작으면_COMMON_400_VALIDATION_FAILED를_반환한다() throws Exception {
        mockMvc.perform(authenticatedSearch()
                        .queryParam("keyword", "아이유")
                        .queryParam("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.result").isEmpty());

        verifyNoInteractions(trackQueryService);
    }

    @Test
    void 인증이_누락되면_COMMON_401_UNAUTHORIZED를_반환한다() throws Exception {
        mockMvc.perform(get(ENDPOINT).queryParam("keyword", "아이유"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON_401_UNAUTHORIZED"))
                .andExpect(jsonPath("$.result").isEmpty());

        verifyNoInteractions(trackQueryService);
    }

    @Test
    void 외부_API_오류는_TRACK_EXTERNAL_API_ERROR와_null_result를_반환한다() throws Exception {
        when(trackQueryService.searchTracks(any()))
                .thenThrow(new TrackException(TrackErrorCode.TRACK_EXTERNAL_API_ERROR));

        mockMvc.perform(authenticatedSearch().queryParam("keyword", "아이유"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("TRACK_EXTERNAL_API_ERROR"))
                .andExpect(jsonPath("$.result").isEmpty());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            authenticatedSearch() {
        return get(ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN);
    }

    private TrackResponse.SearchResult searchResult() {
        return new TrackResponse.SearchResult(List.of(new TrackResponse.Item(
                123L,
                "밤편지",
                "아이유",
                "Palette",
                "https://image.example/cover.jpg",
                "https://audio.example/preview.m4a",
                253000
        )));
    }
}
