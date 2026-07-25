package com.example.plimap.domain.auth.controller;

import com.example.plimap.domain.auth.dto.response.AuthResDTO;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.member.service.command.TermsCommandService;
import com.example.plimap.domain.member.service.query.TermsQueryService;
import com.example.plimap.global.apiPayload.ApiResponse;
import com.example.plimap.global.security.AuthCookieUtil;
import com.example.plimap.global.security.JwtUtil;
import com.example.plimap.global.security.RefreshTokenService;
import com.example.plimap.global.security.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.CsrfToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final AuthController controller = new AuthController(
            mock(MemberCommandService.class),
            mock(TermsQueryService.class),
            mock(TermsCommandService.class),
            mock(MemberRepository.class),
            mock(JwtUtil.class),
            mock(TokenBlacklistService.class),
            mock(RefreshTokenService.class),
            mock(AuthCookieUtil.class)
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
}
