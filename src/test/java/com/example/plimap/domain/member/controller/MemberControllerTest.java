package com.example.plimap.domain.member.controller;

import com.example.plimap.domain.auth.service.command.impl.CustomOAuthService;
import com.example.plimap.domain.auth.service.command.impl.OAuthFailureHandler;
import com.example.plimap.domain.auth.service.command.impl.OAuthSuccessHandler;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.global.apiPayload.exception.GlobalExceptionHandler;
import com.example.plimap.global.config.CorsConfig;
import com.example.plimap.global.config.SecurityConfig;
import com.example.plimap.global.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.plimap.global.security.JwtUtil;
import com.example.plimap.global.security.SecurityErrorResponseHandler;
import com.example.plimap.global.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MemberController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        SecurityErrorResponseHandler.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class MemberControllerTest {

    private static final String ACCESS_TOKEN = "valid-access-token";
    private static final Long AUTH_MEMBER_ID = 1L;
    private static final Long TARGET_MEMBER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomOAuthService customOAuthService;

    @MockitoBean
    private OAuthSuccessHandler oAuthSuccessHandler;

    @MockitoBean
    private OAuthFailureHandler oAuthFailureHandler;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @MockitoBean
    private MemberCommandService memberCommandService;

    @MockitoBean
    private MemberQueryService memberQueryService;

    @BeforeEach
    void setUp() {
        when(jwtUtil.isValid(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.isAccessToken(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.getJti(ACCESS_TOKEN)).thenReturn("test-jti");
        when(tokenBlacklistService.isBlacklisted("test-jti")).thenReturn(false);
        when(jwtUtil.getMemberId(ACCESS_TOKEN)).thenReturn(AUTH_MEMBER_ID);

        Member authenticatedMember = Member.builder().build();
        ReflectionTestUtils.setField(authenticatedMember, "id", AUTH_MEMBER_ID);
        when(memberRepository.findById(AUTH_MEMBER_ID)).thenReturn(Optional.of(authenticatedMember));
    }

    @Test
    void 언팔로우에_성공하면_200과_UNFOLLOWED_응답을_반환한다() throws Exception {
        doNothing().when(memberCommandService).unfollow(AUTH_MEMBER_ID, TARGET_MEMBER_ID);

        mockMvc.perform(delete("/api/v1/members/{memberId}/follow", TARGET_MEMBER_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("MEMBER_200_UNFOLLOWED"))
                .andExpect(jsonPath("$.message").value("언팔로우했습니다."))
                .andExpect(jsonPath("$.result").doesNotExist());

        verify(memberCommandService).unfollow(AUTH_MEMBER_ID, TARGET_MEMBER_ID);
    }

    @Test
    void 팔로우_중이_아닌_회원을_언팔로우하면_404를_반환한다() throws Exception {
        doThrow(new MemberException(MemberErrorCode.NOT_FOLLOWING))
                .when(memberCommandService).unfollow(AUTH_MEMBER_ID, TARGET_MEMBER_ID);

        mockMvc.perform(delete("/api/v1/members/{memberId}/follow", TARGET_MEMBER_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_404_NOT_FOLLOWING"))
                .andExpect(jsonPath("$.message").value("팔로우 중이 아닌 사용자입니다."))
                .andExpect(jsonPath("$.result").doesNotExist());
    }
}
