package com.example.plimap.global.security;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = TokenResolver.resolve(request);

        if (token != null && jwtUtil.isValid(token) && jwtUtil.isAccessToken(token)
                && !tokenBlacklistService.isBlacklisted(jwtUtil.getJti(token))) {
            Long memberId = jwtUtil.getMemberId(token);
            Member member = memberRepository.findById(memberId).orElse(null);

            if (member != null) {
                AuthMember authMember = new AuthMember(member);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(authMember, null, authMember.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
