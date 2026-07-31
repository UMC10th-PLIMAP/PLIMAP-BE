package com.example.plimap.global.security;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberRole;
import com.example.plimap.domain.member.enums.MemberStatus;
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
        when(memberRepository.findByIdAndStatusAndDeletedAtIsNull(MEMBER_ID, MemberStatus.ACTIVE))
                .thenReturn(Optional.of(member));

        jwtAuthFilter.doFilter(request(TOKEN), mock(HttpServletResponse.class), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void 탈퇴한_회원의_토큰이면_블랙리스트에_없어도_인증하지_않는다() throws Exception {
        when(jwtUtil.isValid(TOKEN)).thenReturn(true);
        when(jwtUtil.isAccessToken(TOKEN)).thenReturn(true);
        when(jwtUtil.getJti(TOKEN)).thenReturn(JTI);
        when(tokenBlacklistService.isBlacklisted(JTI)).thenReturn(false);
        when(jwtUtil.getMemberId(TOKEN)).thenReturn(MEMBER_ID);
        when(memberRepository.findByIdAndStatusAndDeletedAtIsNull(MEMBER_ID, MemberStatus.ACTIVE))
                .thenReturn(Optional.empty());

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
        verify(memberRepository, never()).findByIdAndStatusAndDeletedAtIsNull(MEMBER_ID, MemberStatus.ACTIVE);
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
