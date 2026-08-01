package com.example.plimap.domain.auth.controller;

import com.example.plimap.domain.auth.dto.response.AuthResDTO;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.member.service.command.TermsCommandService;
import com.example.plimap.domain.member.service.query.TermsQueryService;
import com.example.plimap.global.apiPayload.ApiResponse;
import com.example.plimap.global.security.AuthCookieUtil;
import com.example.plimap.global.security.JwtUtil;
import com.example.plimap.global.security.RefreshTokenService;
import com.example.plimap.global.security.SessionInvalidationService;
import jakarta.servlet.http.Cookie;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final SessionInvalidationService sessionInvalidationService = mock(SessionInvalidationService.class);

    private final AuthController controller = new AuthController(
            mock(MemberCommandService.class),
            mock(TermsQueryService.class),
            mock(TermsCommandService.class),
            memberRepository,
            jwtUtil,
            refreshTokenService,
            mock(AuthCookieUtil.class),
            sessionInvalidationService
    );

    @Test
    void CSRF_토큰을_응답_본문으로_반환한다() {
        CsrfToken csrfToken = mock(CsrfToken.class);
        when(csrfToken.getToken()).thenReturn("masked-csrf-token");

        ApiResponse<AuthResDTO.CsrfToken> response = controller.getCsrfToken(csrfToken);

        assertThat(response.getIsSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo("AUTH_200_CSRF_TOKEN_ISSUED");
        assertThat(response.getResult().token()).isEqualTo("masked-csrf-token");
    }

    @Test
    void 로그아웃하면_세션_무효화를_위임한다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ApiResponse<Void> apiResponse = controller.logout(request, response);

        assertThat(apiResponse.getIsSuccess()).isTrue();
        assertThat(apiResponse.getCode()).isEqualTo("MEMBER_200_LOGOUT");
        verify(sessionInvalidationService).invalidate(request, response);
    }

    @Test
    void 탈퇴한_회원의_리프레시_토큰이면_재발급을_거부한다() {
        String refreshToken = "refresh-token";
        when(jwtUtil.isValid(refreshToken)).thenReturn(true);
        when(jwtUtil.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtUtil.getMemberId(refreshToken)).thenReturn(1L);
        when(refreshTokenService.matches(1L, refreshToken)).thenReturn(true);
        when(memberRepository.findByIdAndStatusAndDeletedAtIsNull(1L, MemberStatus.ACTIVE))
                .thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", refreshToken));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> controller.reissue(request, response))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
