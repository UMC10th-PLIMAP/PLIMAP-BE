package com.example.plimap.global.security;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberRole;
import com.example.plimap.domain.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

    private static final Long MEMBER_ID = 1L;
    private static final String TOKEN = "valid-token";
    private static final String JTI = "jti-1";

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);

    private final JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtUtil, memberRepository, tokenBlacklistService);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효하고_블랙리스트에_없는_토큰이면_인증에_성공한다() throws Exception {
        Member member = mock(Member.class);
        when(member.getRole()).thenReturn(MemberRole.USER);
        when(jwtUtil.isValid(TOKEN)).thenReturn(true);
        when(jwtUtil.isAccessToken(TOKEN)).thenReturn(true);
        when(jwtUtil.getJti(TOKEN)).thenReturn(JTI);
        when(tokenBlacklistService.isBlacklisted(JTI)).thenReturn(false);
        when(jwtUtil.getMemberId(TOKEN)).thenReturn(MEMBER_ID);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        jwtAuthFilter.doFilter(request(TOKEN), mock(HttpServletResponse.class), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void 정지_탈퇴_등_상태와_무관하게_회원이_존재하면_인증에_성공한다() throws Exception {
        // 상태(SUSPENDED/WITHDRAWN)에 따른 차단은 MemberStatusInterceptor가 담당하므로,
        // JwtAuthFilter는 회원이 DB에 존재하기만 하면 상태와 무관하게 인증을 성공시켜야 한다.
        Member withdrawnMember = mock(Member.class);
        when(withdrawnMember.getRole()).thenReturn(MemberRole.USER);
        when(jwtUtil.isValid(TOKEN)).thenReturn(true);
        when(jwtUtil.isAccessToken(TOKEN)).thenReturn(true);
        when(jwtUtil.getJti(TOKEN)).thenReturn(JTI);
        when(tokenBlacklistService.isBlacklisted(JTI)).thenReturn(false);
        when(jwtUtil.getMemberId(TOKEN)).thenReturn(MEMBER_ID);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(withdrawnMember));

        jwtAuthFilter.doFilter(request(TOKEN), mock(HttpServletResponse.class), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void 회원이_DB에_존재하지_않으면_인증하지_않는다() throws Exception {
        when(jwtUtil.isValid(TOKEN)).thenReturn(true);
        when(jwtUtil.isAccessToken(TOKEN)).thenReturn(true);
        when(jwtUtil.getJti(TOKEN)).thenReturn(JTI);
        when(tokenBlacklistService.isBlacklisted(JTI)).thenReturn(false);
        when(jwtUtil.getMemberId(TOKEN)).thenReturn(MEMBER_ID);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        jwtAuthFilter.doFilter(request(TOKEN), mock(HttpServletResponse.class), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void 블랙리스트에_등록된_토큰이면_인증하지_않는다() throws Exception {
        when(jwtUtil.isValid(TOKEN)).thenReturn(true);
        when(jwtUtil.isAccessToken(TOKEN)).thenReturn(true);
        when(jwtUtil.getJti(TOKEN)).thenReturn(JTI);
        when(tokenBlacklistService.isBlacklisted(JTI)).thenReturn(true);

        jwtAuthFilter.doFilter(request(TOKEN), mock(HttpServletResponse.class), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(memberRepository, never()).findById(MEMBER_ID);
    }

    @Test
    void 서명이_유효하지_않은_토큰이면_인증하지_않는다() throws Exception {
        when(jwtUtil.isValid(TOKEN)).thenReturn(false);

        jwtAuthFilter.doFilter(request(TOKEN), mock(HttpServletResponse.class), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenBlacklistService, never()).isBlacklisted(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void Refresh_Token을_Access_Token으로_사용하면_인증하지_않는다() throws Exception {
        when(jwtUtil.isValid(TOKEN)).thenReturn(true);
        when(jwtUtil.isAccessToken(TOKEN)).thenReturn(false);

        jwtAuthFilter.doFilter(request(TOKEN), mock(HttpServletResponse.class), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenBlacklistService, never()).isBlacklisted(org.mockito.ArgumentMatchers.any());
    }

    private HttpServletRequest request(String token) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("accessToken", token)});
        return request;
    }
}
