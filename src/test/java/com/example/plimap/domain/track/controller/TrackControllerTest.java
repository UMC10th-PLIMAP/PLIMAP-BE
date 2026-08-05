package com.example.plimap.domain.track.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.plimap.domain.auth.service.command.impl.CustomOAuthService;
import com.example.plimap.domain.auth.service.command.impl.OAuthFailureHandler;
import com.example.plimap.domain.auth.service.command.impl.OAuthSuccessHandler;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.track.dto.request.TrackRequest;
import com.example.plimap.domain.track.dto.response.TrackResponse;
import com.example.plimap.domain.track.exception.TrackErrorCode;
import com.example.plimap.domain.track.exception.TrackException;
import com.example.plimap.domain.track.service.command.TrackPlaybackPreparationService;
import com.example.plimap.domain.track.service.query.TrackQueryService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TrackController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        SecurityErrorResponseHandler.class,
        AuthCookieUtil.class,
        HttpCookieOAuth2AuthorizationRequestRepository.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class TrackControllerTest {

    private static final String ENDPOINT = "/api/v1/tracks/search";
    private static final String PLAYBACK_ENDPOINT =
            "/api/v1/tracks/playback-preparations";
    private static final String ACCESS_TOKEN = "valid-access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrackQueryService trackQueryService;

    @MockitoBean
    private TrackPlaybackPreparationService trackPlaybackPreparationService;

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

    @Test
    void 인증된_구간_재생_준비_요청은_성공_응답을_반환한다() throws Exception {
        when(trackPlaybackPreparationService.prepare(any()))
                .thenReturn(playbackResponse());

        mockMvc.perform(authenticatedPlayback("{\"itunesTrackId\":123}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code")
                        .value("TRACK_PLAYBACK_PREPARATION_SUCCESS"))
                .andExpect(jsonPath("$.message").value("구간 재생 준비에 성공했습니다."))
                .andExpect(jsonPath("$.result.itunesTrackId").value(123))
                .andExpect(jsonPath("$.result.youtubeVideoId").value("abcdefghijk"))
                .andExpect(jsonPath("$.result.albumImageUrl")
                        .value("https://image.example/cover.jpg"));

        verify(trackPlaybackPreparationService)
                .prepare(new TrackRequest.PlaybackPreparation(123L));
    }

    @Test
    void itunesTrackId가_null이면_validation_실패를_반환한다() throws Exception {
        assertPlaybackValidation("{\"itunesTrackId\":null}", "iTunes 트랙 ID를 입력해주세요.");
    }

    @Test
    void itunesTrackId가_0이면_validation_실패를_반환한다() throws Exception {
        assertPlaybackValidation("{\"itunesTrackId\":0}", "iTunes 트랙 ID는 양수여야 합니다.");
    }

    @Test
    void itunesTrackId가_음수이면_validation_실패를_반환한다() throws Exception {
        assertPlaybackValidation("{\"itunesTrackId\":-1}", "iTunes 트랙 ID는 양수여야 합니다.");
    }

    @Test
    void metadata_만료는_Track_도메인_오류_응답을_반환한다() throws Exception {
        assertPlaybackError(
                TrackErrorCode.TRACK_METADATA_CACHE_NOT_FOUND,
                "TRACK_404_METADATA_CACHE_NOT_FOUND",
                404
        );
    }

    @Test
    void YouTube_매칭_실패는_Track_도메인_오류_응답을_반환한다() throws Exception {
        assertPlaybackError(
                TrackErrorCode.YOUTUBE_MATCH_NOT_FOUND,
                "TRACK_404_YOUTUBE_MATCH_NOT_FOUND",
                404
        );
    }

    @Test
    void YouTube_외부_API_오류는_Track_도메인_오류_응답을_반환한다() throws Exception {
        assertPlaybackError(
                TrackErrorCode.YOUTUBE_EXTERNAL_API_ERROR,
                "TRACK_500_YOUTUBE_EXTERNAL_API_ERROR",
                500
        );
    }

    @Test
    void 미인증_구간_재생_준비_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(post(PLAYBACK_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itunesTrackId\":123}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON_401_UNAUTHORIZED"));

        verifyNoInteractions(trackPlaybackPreparationService);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            authenticatedSearch() {
        return get(ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            authenticatedPlayback(String body) {
        return post(PLAYBACK_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private void assertPlaybackValidation(String body, String message) throws Exception {
        mockMvc.perform(authenticatedPlayback(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.result").isEmpty());

        verifyNoInteractions(trackPlaybackPreparationService);
    }

    private void assertPlaybackError(
            TrackErrorCode errorCode,
            String expectedCode,
            int expectedStatus
    ) throws Exception {
        when(trackPlaybackPreparationService.prepare(any()))
                .thenThrow(new TrackException(errorCode));

        mockMvc.perform(authenticatedPlayback("{\"itunesTrackId\":123}"))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.result").isEmpty());
    }

    private TrackResponse.TrackSearchResult searchResult() {
        return new TrackResponse.TrackSearchResult(List.of(
                new TrackResponse.TrackSearchItem(
                123L,
                "밤편지",
                "아이유",
                "Palette",
                "https://image.example/cover.jpg",
                "https://audio.example/preview.m4a",
                253000
                )));
    }

    private TrackResponse.PlaybackPreparationResult playbackResponse() {
        return new TrackResponse.PlaybackPreparationResult(
                123L,
                "abcdefghijk",
                "밤편지",
                "아이유",
                "Palette",
                "https://image.example/cover.jpg",
                "https://audio.example/preview.m4a",
                253000
        );
    }
}
