package com.example.plimap.domain.auth.service.command.impl;

import com.example.plimap.domain.auth.exception.SanctionedMemberAuthenticationException;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.report.enums.ReportCategory;
import com.example.plimap.global.config.OAuthProperties;
import com.example.plimap.global.security.AuthCookieUtil;
import com.example.plimap.global.security.OAuthFrontendRedirectCookieRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthFailureHandlerTest {

    private final AuthCookieUtil authCookieUtil = new AuthCookieUtil();
    private final OAuthFrontendRedirectCookieRepository redirectCookieRepository =
            new OAuthFrontendRedirectCookieRepository(
                    authCookieUtil,
                    new OAuthProperties(
                            "https://dev.plimap.kr/home",
                            List.of("https://dev.plimap.kr", "http://localhost:5173")
                    )
            );
    private final OAuthFailureHandler handler = new OAuthFailureHandler(redirectCookieRepository);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authCookieUtil, "cookieSecure", true);
        ReflectionTestUtils.setField(authCookieUtil, "cookieSameSite", "None");
    }

    @Test
    void 로컬에서_시작한_인증_실패는_로컬에_에러_파라미터를_붙여_리다이렉트한다() throws Exception {
        MockHttpServletRequest request = callbackRequestFor("http://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, authenticationException());

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/home?error=oauth_login_failed");
    }

    @Test
    void Dev에서_시작한_인증_실패는_Dev에_에러_파라미터를_붙여_리다이렉트한다() throws Exception {
        MockHttpServletRequest request = callbackRequestFor("https://dev.plimap.kr");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, authenticationException());

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://dev.plimap.kr/home?error=oauth_login_failed");
    }

    private MockHttpServletRequest callbackRequestFor(String frontendOrigin) {
        MockHttpServletRequest authorizationRequest = new MockHttpServletRequest();
        authorizationRequest.addParameter("frontendOrigin", frontendOrigin);
        MockHttpServletResponse authorizationResponse = new MockHttpServletResponse();
        redirectCookieRepository.saveRequestedOrigin(
                authorizationRequest,
                authorizationResponse,
                "test-state"
        );
        Cookie originCookie = authorizationResponse.getCookie("oauth2_frontend_origin");
        assertThat(originCookie).isNotNull();

        MockHttpServletRequest callbackRequest = new MockHttpServletRequest();
        callbackRequest.setCookies(originCookie);
        callbackRequest.addParameter("state", "test-state");
        return callbackRequest;
    }

    @Test
    void 벌점으로_탈퇴된_회원의_재가입_시도는_사유_정보와_함께_전용_에러_파라미터로_리다이렉트한다() throws Exception {
        MockHttpServletRequest request = callbackRequestFor("http://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Member member = withdrawnMember(ReportCategory.ABUSE_OR_HATE_SPEECH, "욕설 반복 신고 누적");

        handler.onAuthenticationFailure(request, response, new SanctionedMemberAuthenticationException(member));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/home?error=account_permanently_banned"
                        + "&reasonCategory=ABUSE_OR_HATE_SPEECH&reasonDetail=%EC%9A%95%EC%84%A4%20%EB%B0%98%EB%B3%B5%20%EC%8B%A0%EA%B3%A0%20%EB%88%84%EC%A0%81");
    }

    @Test
    void 정지_중인_회원의_로그인_시도는_해제일과_함께_전용_에러_파라미터로_리다이렉트한다() throws Exception {
        MockHttpServletRequest request = callbackRequestFor("http://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Instant suspendedUntil = Instant.parse("2026-08-20T00:00:00Z");
        Member member = suspendedMember(ReportCategory.OBSCENE_OR_HARMFUL, null, suspendedUntil);

        handler.onAuthenticationFailure(request, response, new SanctionedMemberAuthenticationException(member));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/home?error=account_suspended"
                        + "&reasonCategory=OBSCENE_OR_HARMFUL&suspendedUntil=2026-08-20T00:00:00Z");
    }

    private AuthenticationException authenticationException() {
        return new OAuth2AuthenticationException(
                new OAuth2Error("invalid_grant"),
                "invalid_grant"
        );
    }

    private Member withdrawnMember(ReportCategory reasonCategory, String reasonDetail) {
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "status", MemberStatus.WITHDRAWN);
        ReflectionTestUtils.setField(member, "lastPenaltyCategory", reasonCategory);
        ReflectionTestUtils.setField(member, "lastPenaltyDetail", reasonDetail);
        return member;
    }

    private Member suspendedMember(ReportCategory reasonCategory, String reasonDetail, Instant suspendedUntil) {
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "status", MemberStatus.SUSPENDED);
        ReflectionTestUtils.setField(member, "lastPenaltyCategory", reasonCategory);
        ReflectionTestUtils.setField(member, "lastPenaltyDetail", reasonDetail);
        ReflectionTestUtils.setField(member, "suspendedUntil", suspendedUntil);
        return member;
    }
}
